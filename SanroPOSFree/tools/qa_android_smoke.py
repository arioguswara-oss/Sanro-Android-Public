#!/usr/bin/env python3
"""Android UI smoke QA against a fresh synthetic-only debug emulator.

Uses adb/UIAutomator bounds rather than guessed screenshot coordinates.
Never provides credentials, contacts a production endpoint or tests physical BT.
Artifacts contain only test-generated sandbox UI and compact structured results.
"""
import argparse
import hashlib
import json
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PKG = "com.sanrotech.pospublic.uifixdebug"
ACTIVITY = "com.sanrotech.pospublic.MainActivity"
BOUNDS = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")
STEPS = []


def adb(*args, timeout=35, binary=False):
    p = subprocess.run(["adb", *args], capture_output=True, timeout=timeout)
    if p.returncode:
        raise RuntimeError(f"adb {' '.join(args[:3])} failed ({p.returncode}): "
                           + p.stderr.decode("utf-8", "replace")[-250:])
    return p.stdout if binary else p.stdout.decode("utf-8", "replace")


def screen_tree():
    # UIAutomator may transiently fail while Android is resizing/transiting.
    adb("shell", "uiautomator", "dump", "/sdcard/sanro-qa-window.xml", timeout=40)
    xml = adb("exec-out", "cat", "/sdcard/sanro-qa-window.xml")
    return ET.fromstring(xml)


def nodes():
    return list(screen_tree().iter("node"))


def text_of(node):
    return (node.get("text", "") + " " + node.get("content-desc", "")).strip()


def matches(needle, results):
    key = needle.casefold()
    return [n for n in results if key in text_of(n).casefold()]


def wait_text(needle, timeout=40):
    last_error = None
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        try:
            found = matches(needle, nodes())
            if found:
                STEPS.append("VISIBLE: " + needle)
                return found
        except (RuntimeError, ET.ParseError) as exc:
            last_error = str(exc)
        time.sleep(1.5)
    raise AssertionError("Missing Android UI text: " + needle +
                         (f"; UI transient: {last_error}" if last_error else ""))


def tap_node(node):
    match = BOUNDS.fullmatch(node.get("bounds", ""))
    if not match:
        raise AssertionError("UI node has no valid bounds")
    x1, y1, x2, y2 = map(int, match.groups())
    if x2 <= x1 or y2 <= y1:
        raise AssertionError("UI node bounds are empty")
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(0.5)


def tap_button(needle):
    found = wait_text(needle)
    buttons = [x for x in found if x.get("class", "").endswith("Button")]
    if not buttons:
        raise AssertionError("Expected a visible Button: " + needle)
    tap_node(buttons[0])
    STEPS.append("TAP: " + needle)


def fill_dialog(values):
    # Android native AlertDialog's four EditTexts, in deterministic form order.
    end = time.monotonic() + 15
    while True:
        candidates = [n for n in nodes() if n.get("class", "").endswith("EditText")]
        if len(candidates) >= len(values):
            break
        if time.monotonic() > end:
            raise AssertionError("Expected native input fields not visible")
        time.sleep(1)
    for index, value in enumerate(values):
        candidates = [n for n in nodes() if n.get("class", "").endswith("EditText")]
        if index >= len(candidates):
            raise AssertionError("Dialog input list changed unexpectedly")
        tap_node(candidates[index])
        adb("shell", "input", "text", value)
        adb("shell", "input", "keyevent", "4")  # Hide software keyboard.
        time.sleep(0.5)


def screenshot(folder, name):
    (folder / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p", binary=True))


def app_alive():
    result = adb("shell", "pidof", PKG)
    if not result.strip():
        raise AssertionError("App process not running")


def validate_no_app_crash():
    # Inspect only app-specific process marker; do not publish full logcat
    # (which could contain unrelated device logs).
    crash = adb("logcat", "-b", "crash", "-d", timeout=20)
    if f"Process: {PKG}" in crash:
        raise AssertionError("AndroidRuntime recorded a crash in QA app")
    STEPS.append("APP_CRASH=NONE_DETECTED")


def launch():
    adb("shell", "am", "start", "-n", f"{PKG}/{ACTIVITY}", timeout=25)


def qa(apk, folder):
    folder.mkdir(parents=True, exist_ok=True)
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    adb("devices")
    adb("install", "-r", str(apk), timeout=100)
    adb("logcat", "-b", "crash", "-c")
    launch()
    wait_text("Buka sandbox lokal")
    wait_text("Masuk POS2")
    screenshot(folder, "01_login_locked")
    # Verify the native login cannot bypass the absent staging authority.
    tap_button("Masuk POS2")
    tap_button("Verifikasi akun")
    wait_text("tidak ada sesi atau tenant yang diaktifkan")
    screenshot(folder, "02_auth_fail_closed")
    tap_button("Kembali")
    tap_button("Buka sandbox lokal")
    wait_text("SANDBOX DEBUG")
    wait_text("Kasir")
    wait_text("Barang")
    wait_text("Riwayat")
    screenshot(folder, "03_sandbox_cashier")

    # One completely synthetic local product + cash checkout, no server writes.
    tap_button("Barang")
    tap_button("+ Tambah barang")
    fill_dialog(["QAEMULATOR001", "QAEMULATORITEM", "3500", "5"])
    tap_button("Simpan")
    wait_text("QAEMULATORITEM")
    screenshot(folder, "04_local_product")

    tap_button("Kasir")
    tap_button("QAEMULATORITEM")
    wait_text("Bayar cash")
    tap_button("Bayar cash")
    fill_dialog(["10000"])
    tap_button("Simpan transaksi")
    wait_text("Nota tersimpan", timeout=50)
    wait_text("QAEMULATORITEM", timeout=10)
    screenshot(folder, "05_committed_sale")
    tap_button("Tutup")
    app_alive()

    # Force close and reopen with internet off: persisted Room history retained.
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "svc", "wifi", "disable")
    adb("shell", "svc", "data", "disable")
    launch()
    wait_text("Buka sandbox lokal")
    tap_button("Buka sandbox lokal")
    wait_text("SANDBOX DEBUG")
    tap_button("Riwayat")
    wait_text("No. uji:", timeout=35)
    screenshot(folder, "06_offline_reopen_history")
    app_alive()
    validate_no_app_crash()
    result = {
        "status": "PASS", "package": PKG, "emulator_only": True,
        "apk_sha256": digest, "checks": STEPS,
        "production_login_tested": False, "physical_printer_tested": False
    }
    (folder / "summary.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
    print("ANDROID_EMULATOR_QA=PASS")
    print("APK_SHA256=" + digest)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--apk", type=Path, required=True)
    p.add_argument("--out-dir", type=Path, required=True)
    args = p.parse_args()
    try:
        if not args.apk.is_file():
            raise FileNotFoundError("APK artifact missing")
        qa(args.apk, args.out_dir)
    except (AssertionError, RuntimeError, subprocess.TimeoutExpired,
            ET.ParseError, OSError) as exc:
        args.out_dir.mkdir(parents=True, exist_ok=True)
        try:
            screenshot(args.out_dir, "failure_screen")
        except Exception:
            pass
        (args.out_dir / "summary.json").write_text(json.dumps({
            "status": "FAIL", "error": str(exc), "checks": STEPS,
            "production_login_tested": False, "physical_printer_tested": False
        }, indent=2), encoding="utf-8")
        print("ANDROID_EMULATOR_QA=FAIL: " + str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
