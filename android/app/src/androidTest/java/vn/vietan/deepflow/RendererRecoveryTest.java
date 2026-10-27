package vn.vietan.deepflow;

import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Kết thúc renderer thật, không giả lập callback bằng mock. */
@RunWith(AndroidJUnit4.class)
public class RendererRecoveryTest {
    @Test public void foregroundRendererCrashRecreatesActivityWithoutKillingHost() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<MainActivity> first = new AtomicReference<>();
            scenario.onActivity(activity -> first.set(activity));
            SystemClock.sleep(4000);
            scenario.onActivity(activity -> activity.getBridge().getWebView().loadUrl("chrome://crash"));
            long until = SystemClock.elapsedRealtime() + 20000;
            AtomicReference<MainActivity> current = new AtomicReference<>();
            do {
                SystemClock.sleep(250);
                scenario.onActivity(current::set);
            } while (current.get() == first.get() && SystemClock.elapsedRealtime() < until);
            assertNotSame("Activity phải được dựng mới sau renderer crash", first.get(), current.get());
            scenario.onActivity(activity -> {
                assertNotNull(activity.getBridge());
                assertNotNull(activity.getBridge().getWebView());
                assertFalse(MainActivity.playing);
            });
        }
    }
}
