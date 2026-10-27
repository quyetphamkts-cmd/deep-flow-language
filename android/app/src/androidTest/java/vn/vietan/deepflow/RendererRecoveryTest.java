package vn.vietan.deepflow;

import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.Before;
import androidx.lifecycle.Lifecycle;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Kết thúc renderer thật, không giả lập callback bằng mock. */
@RunWith(AndroidJUnit4.class)
public class RendererRecoveryTest {
    @Before public void grantNotificationPermission() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
            .grantRuntimePermission("vn.vietan.deepflow", "android.permission.POST_NOTIFICATIONS");
    }

    @Test public void backgroundRendererDeathWaitsForResumeThenRecovers() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<MainActivity> first = new AtomicReference<>();
            scenario.onActivity(first::set);
            SystemClock.sleep(4000);
            scenario.moveToState(Lifecycle.State.CREATED);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                first.get().getBridge().getWebView().loadUrl("chrome://crash"));
            SystemClock.sleep(1500);
            assertFalse("Không tự phát sau crash", MainActivity.playing);
            scenario.moveToState(Lifecycle.State.RESUMED);
            AtomicReference<MainActivity> current = new AtomicReference<>();
            long until = SystemClock.elapsedRealtime() + 20000;
            do {
                SystemClock.sleep(250);
                scenario.onActivity(current::set);
            } while (current.get() == first.get() && SystemClock.elapsedRealtime() < until);
            assertNotSame("Mở lại app phải tạo màn hình mới", first.get(), current.get());
        }
    }

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
