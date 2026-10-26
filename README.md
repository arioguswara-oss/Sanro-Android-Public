# SANRO Android — Public Source

This repository contains a **carefully scoped, public Android client** for SANRO products.

Current module: **SANRO Superadmin Android V1.0.0 — candidate** at `SanroSuperadmin/`. The APK is intended for **private installation** by authorized operators; source visibility does not grant access to the SANRO backend.

This repository does **not** contain the private legacy `Sanro-Android` history, POS/Stock snapshots, production environment files, server code, database, credentials, certificates, signing keys, or tenant data.

## Build
The standalone app is at `SanroSuperadmin/`. GitHub Actions compiles a **debug APK**, runs unit tests and Android lint. Successful artifacts are for authorized testing only; they are not release-signed APKs. No APK may be called production-ready without on-device authenticated QA and Owner sign-off.

## License and security
Copyright © SANRO. **No open-source license is granted by this repository.** Public access to view the source does not authorize reuse/distribution of proprietary assets. Please report security issues privately instead of posting exploitable details in public issues. Never publish signing keys, passwords, session cookies, tokens or any production secrets.
