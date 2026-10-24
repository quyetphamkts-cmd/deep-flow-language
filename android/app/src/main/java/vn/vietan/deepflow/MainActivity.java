package vn.vietan.deepflow;

import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.ViewGroup;
import android.os.Build;

import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.WebViewListener;

public class MainActivity extends BridgeActivity {
    /** Ứng dụng đang hiện trên màn hình (dùng để quyết định có cần hiện thông báo tai nghe hay không) */
    public static volatile boolean isForeground = false;
    /** Được mở từ thông báo "tai nghe đã kết nối" → phát tiếp khi app sẵn sàng */
    public static volatile boolean pendingAutoplay = false;
    /** Đang phát nhạc (do app web báo qua plugin Headset) */
    public static volatile boolean playing = false;
    /** Người dùng cho phép phát dưới nền khi rời app (Cài đặt → Chạy nền) */
    public static volatile boolean bgPlay = true;
    /** Phiên cũ vẫn đang phát sau khi người dùng vuốt tắt app */
    private static Bridge orphan = null;
    /** WebView bị Android đóng khi app ở nền → mở lại app thì dựng lại màn hình */
    private static volatile boolean needRecreate = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(GDrivePlugin.class);
        registerPlugin(HeadsetPlugin.class);
        super.onCreate(savedInstanceState);
        // lỗi Java làm app bị tắt: ghi lại tên lỗi + vài dòng vị trí vào nhật ký để lần sau xem được
        final Thread.UncaughtExceptionHandler prevH = Thread.getDefaultUncaughtExceptionHandler();
        if (!(prevH instanceof DfCrashHandler)) Thread.setDefaultUncaughtExceptionHandler(new DfCrashHandler(getApplicationContext(), prevH));
        NativeDiag.log(this, "Màn hình app được tạo" + (savedInstanceState != null ? " (khôi phục sau khi Android đóng màn hình)" : "")
            + (orphan != null ? " · phiên cũ vẫn đang phát nền → chuyển sang phiên mới" : ""));
        stopOrphan();
        try {
            getBridge().addWebViewListener(new WebViewListener() {
                @Override
                public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
                    boolean crash = Build.VERSION.SDK_INT >= 26 && d != null && d.didCrash();
                    NativeDiag.log(MainActivity.this, "WebView bị đóng (" + (crash ? "lỗi" : "Android thu hồi bộ nhớ")
                        + ", app " + (isForeground ? "đang mở" : "ở nền") + (playing ? ", đang phát" : "") + ") → dựng lại app");
                    try {
                        if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
                        v.destroy();
                    } catch (Exception ignored) {}
                    if (!isForeground || bridge == null || isFinishing() || isDestroyed()) {
                        playing = false;
                        try { Class.forName("io.github.jofr.capacitor.mediasessionplugin.MediaSessionService").getMethod("dfKill").invoke(null); } catch (Exception ignored) {}
                        NativeDiag.log(MainActivity.this, "Đã tắt bảng phát nhạc trên thông báo (nhạc đã dừng vì Android đóng WebView) — mở lại app để nghe tiếp");
                    }
                    if (bridge == null || isFinishing() || isDestroyed()) return true; // phiên cũ đã tách ra
                    if (isForeground) runOnUiThread(MainActivity.this::recreate); else needRecreate = true;
                    return true; // xử lý xong: không để cả app bị tắt theo
                }
            });
        } catch (Exception ignored) {}
        readAutoplay(getIntent());
        try {
            WebSettings s = getBridge().getWebView().getSettings();
            s.setMediaPlaybackRequiresUserGesture(false);
        } catch (Exception ignored) {}
        // Mặc định khi app không hiện trên màn hình, tiến trình hiển thị của WebView (chạy nhạc + JavaScript) bị hạ xuống "ít quan trọng"
        // → máy thiếu RAM là Android đóng nó trước: nhạc tắt, bảng điều khiển trên thông báo bị treo. Giữ ở mức QUAN TRỌNG cả khi ẩn.
        try { if (Build.VERSION.SDK_INT >= 26) getBridge().getWebView().setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false); } catch (Exception ignored) {}
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
        NativeDiag.log(this, "Màn hình app bị đóng" + (isFinishing() ? " (thoát / vuốt tắt)" : " (Android thu hồi màn hình)")
            + (playing ? " · đang phát" + (bgPlay ? " → giữ phát nền" : "") : ""));
        // Vuốt tắt app khi đang phát → giữ WebView sống để nhạc tiếp tục (dịch vụ nền giữ app không bị tắt)
        if (playing && bgPlay && !isChangingConfigurations() && bridge != null && orphan == null) {
            orphan = bridge;
            bridge = null;
        }
        // Vuốt tắt / thoát app khi KHÔNG phát (đã tạm dừng) → tắt hẳn dịch vụ phát nền + bảng phát nhạc, không để app sót lại dưới nền
        else if (isFinishing() && !playing && !isChangingConfigurations()) {
            try { Class.forName("io.github.jofr.capacitor.mediasessionplugin.MediaSessionService").getMethod("dfKill").invoke(null); } catch (Exception ignored) {}
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
    public void onResume() {
        super.onResume();
        isForeground = true;
        if (needRecreate) { needRecreate = false; recreate(); }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level != 20) NativeDiag.log(this, "Android báo thiếu bộ nhớ: " + NativeDiag.trimName(level) + (playing ? " · đang phát" : ""));
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        NativeDiag.log(this, "Android báo máy hết bộ nhớ (onLowMemory)" + (playing ? " · đang phát" : ""));
    }

    @Override
    public void onPause() { super.onPause(); isForeground = false; keepWebViewAlive(); }

    @Override
    public void onStop() {
        super.onStop();
        // Tắt "phát dưới nền": rời app (không phải chỉ tắt màn hình) thì dừng phát
        if (!bgPlay && playing && !isChangingConfigurations()) {
            boolean screenOn = true;
            try { screenOn = ((PowerManager) getSystemService(POWER_SERVICE)).isInteractive(); } catch (Exception ignored) {}
            if (screenOn) {
                try { getBridge().getWebView().evaluateJavascript("try{audio.pause()}catch(e){}", null); } catch (Exception ignored) {}
            }
        }
        keepWebViewAlive();
    }

    /** Giữ JavaScript (hẹn giờ, lặp câu, chuyển bài) tiếp tục chạy khi app ở dưới nền */
    private void keepWebViewAlive() {
        try { getBridge().getWebView().onResume(); getBridge().getWebView().resumeTimers(); } catch (Exception ignored) {}
    }

    /** Ghi lỗi Java vào nhật ký lõi Android trước khi app bị tắt */
    static class DfCrashHandler implements Thread.UncaughtExceptionHandler {
        private final android.content.Context ctx; private final Thread.UncaughtExceptionHandler prev;
        DfCrashHandler(android.content.Context c, Thread.UncaughtExceptionHandler p) { ctx = c; prev = p; }
        @Override public void uncaughtException(Thread t, Throwable e) {
            try {
                StringBuilder b = new StringBuilder("Lỗi Java làm app tắt: ").append(e);
                StackTraceElement[] st = e.getStackTrace();
                for (int i = 0; i < Math.min(6, st.length); i++) b.append(" | at ").append(st[i]);
                Throwable c = e.getCause();
                if (c != null) { b.append(" | do: ").append(c); StackTraceElement[] cs = c.getStackTrace(); for (int i = 0; i < Math.min(3, cs.length); i++) b.append(" | at ").append(cs[i]); }
                NativeDiag.logNow(ctx, b.toString());
            } catch (Throwable ignored) {}
            if (prev != null) prev.uncaughtException(t, e);
        }
    }
}
