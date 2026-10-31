package com.sanrotech.pospublic;

import android.widget.Button;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ActivationGateTest {
    @Test public void launchAndRecreationNeverOpenOperationalDatabase() throws Exception {
        try (var controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertLocked(controller.get());
            controller.recreate();
            assertLocked(controller.get());
        }
    }
    private void assertLocked(MainActivity activity) throws Exception {
        for (String name : new String[]{"db", "tenant"}) {
            Field field = MainActivity.class.getDeclaredField(name);
            field.setAccessible(true);
            assertNull(name, field.get(activity));
        }
        View root = activity.getWindow().getDecorView();
        assertTrue(button(root, "Masuk POS2").isEnabled());
        assertTrue(button(root, "Pulihkan perangkat").isEnabled());
        assertNull(button(root, "Bayar cash"));
    }
    private Button button(View view, String label) {
        if (view instanceof Button && label.contentEquals(((Button)view).getText())) return (Button)view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = button(group.getChildAt(i), label);
                if (found != null) return found;
            }
        }
        return null;
    }
}
