package vn.vietan.deepflow;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;

import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    /** Ứng dụng đang hiện trên màn hình (dùng để quyết định có cần hiện thông báo tai nghe hay không) */
    public static volatile boolean isForeground = false;
    /** Được mở từ thông báo "tai nghe đã kết nối" → phát tiếp khi app sẵn sàng */
    public static volatile boolean pendingAutoplay = false;
    /** Đang phát nhạc (do app web báo qua plugin Headset) */
    public static volatile boolean playing = false;
    /** Phiên cũ vẫn đang phát sau khi người dùng vuốt tắt app */
    private static Bridge orphan = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(GDrivePlugin.class);
        registerPlugin(HeadsetPlugin.class);
        super.onCreate(savedInstanceState);
        stopOrphan();
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

    /** Mở lại app khi phiên cũ còn đang phát: dừng phiên cũ (vị trí đã lưu), phiên mới tự phát tiếp */
    private void stopOrphan() {
        final Bridge o = orphan;
        orphan = null;
        if (o == null) return;
        try { o.getWebView().evaluateJavascript("try{audio.pause()}catch(e){}", null); } catch (Exception ignored) {}
        o.getWebView().postDelayed(() -> {
            try { o.onDestroy(); } catch (Exception ignored) {}
            try { o.getWebView().destroy(); } catch (Exception ignored) {}
        }, 800);
    }

    @Override
    public void onDestroy() {
        // Vuốt tắt app khi đang phát → giữ WebView sống để nhạc tiếp tục (dịch vụ nền giữ app không bị tắt)
        if (playing && isFinishing() && bridge != null && orphan == null) {
            orphan = bridge;
            bridge = null;
        }
        super.onDestroy();
    }

    @Override
    public void onDetachedFromWindow() {
        if (bridge == null) return; // WebView đang được giữ để phát nền
        super.onDetachedFromWindow();
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
