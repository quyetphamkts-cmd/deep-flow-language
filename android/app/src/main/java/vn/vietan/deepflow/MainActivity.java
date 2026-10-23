package vn.vietan.deepflow;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    /** Ứng dụng đang hiện trên màn hình (dùng để quyết định có cần hiện thông báo tai nghe hay không) */
    public static volatile boolean isForeground = false;
    /** Được mở từ thông báo "tai nghe đã kết nối" → phát tiếp khi app sẵn sàng */
    public static volatile boolean pendingAutoplay = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(GDrivePlugin.class);
        registerPlugin(HeadsetPlugin.class);
        super.onCreate(savedInstanceState);
        readAutoplay(getIntent());
        try {
            WebSettings s = getBridge().getWebView().getSettings();
            s.setMediaPlaybackRequiresUserGesture(false);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        readAutoplay(intent);
        if (pendingAutoplay) {
            try { getBridge().triggerWindowJSEvent("dfAutoplay"); } catch (Exception ignored) {}
        }
    }

    private void readAutoplay(Intent intent) {
        if (intent != null && intent.getBooleanExtra("dfAutoplay", false)) {
            pendingAutoplay = true;
            intent.removeExtra("dfAutoplay");
        }
    }

    @Override
    public void onResume() { super.onResume(); isForeground = true; }

    @Override
    public void onPause() { super.onPause(); isForeground = false; keepWebViewAlive(); }

    @Override
    public void onStop() { super.onStop(); keepWebViewAlive(); }

    /** Giữ JavaScript (hẹn giờ, lặp câu, chuyển bài) tiếp tục chạy khi app ở dưới nền */
    private void keepWebViewAlive() {
        try { getBridge().getWebView().onResume(); getBridge().getWebView().resumeTimers(); } catch (Exception ignored) {}
    }
}
