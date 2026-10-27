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
    private static MainActivity orphan = null;
    /** WebView bị Android đóng khi app ở nền → mở lại app thì dựng lại màn hình */
    private boolean rendererGone = false;
    private boolean bridgeDisposed = false;
    private boolean recreatePosted = false;
    private static long recoveryWindowAt;
    private static int recoveryCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // lỗi Java làm app bị tắt: ghi lại tên lỗi + vài dòng vị trí vào nhật ký để lần sau xem được
        final Thread.UncaughtExceptionHandler prevH = Thread.getDefaultUncaughtExceptionHandler();
        if (!(prevH instanceof DfCrashHandler)) Thread.setDefaultUncaughtExceptionHandler(new DfCrashHandler(getApplicationContext(), prevH));
        registerPlugin(GDrivePlugin.class);
        registerPlugin(HeadsetPlugin.class);
        registerPlugin(GemmaPlugin.class);
        registerPlugin(ApkUpdatePlugin.class);
        // Dọn phiên cũ TRƯỚC khi tạo WebView / nối MediaSession mới.
        stopOrphan();
        super.onCreate(savedInstanceState);
        NativeDiag.log(this, "Màn hình app được tạo" + (savedInstanceState != null ? " (khôi phục sau khi Android đóng màn hình)" : "")
            + (orphan != null ? " · phiên cũ vẫn đang phát nền → chuyển sang phiên mới" : ""));
        try {
            getBridge().addWebViewListener(new WebViewListener() {
                @Override
                public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
                    // Một renderer có thể dùng chung cho nhiều WebView. Mỗi owner chỉ dọn một lần.
                    if (rendererGone || bridgeDisposed) return true;
                    rendererGone = true;
                    boolean crash = Build.VERSION.SDK_INT >= 26 && d != null && d.didCrash();
                    NativeDiag.log(MainActivity.this, "WebView bị đóng (" + (crash ? "renderer crash" : "hệ thống kết thúc renderer, chưa xác định có thiếu RAM hay không")
                        + ", app " + (isForeground ? "đang mở" : "ở nền") + (playing ? ", đang phát" : "") + ")");
                    playing = false;
                    pendingAutoplay = false; // không tự phát lại sau crash thành vòng lặp
                    try { Class.forName("io.github.jofr.capacitor.mediasessionplugin.MediaSessionService").getMethod("dfKill").invoke(null); } catch (Exception ignored) {}
                    if (orphan == MainActivity.this) orphan = null;
                    // Ngắt plugin trước khi hủy WebView, tránh nút thông báo / receiver gọi vào phiên chết.
                    disposeBridge();
                    if (!isFinishing() && !isDestroyed() && isForeground) scheduleRecreate();
                    return true; // xử lý xong: không để cả app bị tắt theo
                }
            });
        } catch (Exception ignored) {}
        readAutoplay(getIntent());
        try {
            WebSettings s = getBridge().getWebView().getSettings();
            s.setMediaPlaybackRequiresUserGesture(false);
        } catch (Exception ignored) {}
        // Chỉ giữ mức quan trọng khi đang phát; phiên tạm dừng cho phép Android thu hồi renderer.
        updateRendererPriority();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        readAutoplay(intent);
        if (pendingAutoplay && !bridgeDisposed && !rendererGone) {
            try { getBridge().triggerWindowJSEvent("dfAutoplay"); } catch (Exception ignored) {}
        }
    }

    /** Mở lại app khi phiên cũ còn đang phát: dừng phiên cũ (vị trí đã lưu), phiên mới tự phát tiếp */
    private void stopOrphan() {
        final MainActivity old = orphan;
        orphan = null;
        if (old == null) return;
        old.disposeBridge();
        playing = false;
    }

    /** Chỉ một nơi sở hữu việc tháo plugin và hủy WebView. Chạy trên UI thread. */
    private void disposeBridge() {
        if (bridgeDisposed || bridge == null) return;
        bridgeDisposed = true;
        final Bridge old = bridge;
        final WebView view = old.getWebView();
        try { old.onDestroy(); } catch (Exception e) { NativeDiag.log(this, "Dọn bridge: " + e.getClass().getSimpleName()); }
        try {
            if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
            view.removeAllViews();
            view.destroy();
        } catch (Exception e) { NativeDiag.log(this, "Dọn WebView: " + e.getClass().getSimpleName()); }
    }

    private void scheduleRecreate() {
        if (recreatePosted || isFinishing() || isDestroyed()) return;
        recreatePosted = true;
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - recoveryWindowAt > 60000) { recoveryWindowAt = now; recoveryCount = 0; }
        if (++recoveryCount > 2) {
            NativeDiag.log(this, "Dừng tự dựng WebView: lỗi lặp lại, cần mở lại thủ công");
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Trình phát đã bị gián đoạn")
                .setMessage("WebView bị đóng nhiều lần. Hãy cập nhật Android System WebView và Chrome. Bạn có thể thử mở lại ứng dụng; dữ liệu bài học vẫn được giữ.")
                .setPositiveButton("Mở lại", (dialog, which) -> { recoveryCount = 0; recreatePosted = false; scheduleRecreate(); })
                .setNegativeButton("Đóng", (dialog, which) -> finish()).show();
            return;
        }
        // Chờ callback Chromium trả về trước khi tạo renderer mới.
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (!isFinishing() && !isDestroyed()) recreate();
        });
    }

    /** Không gửi vòng đời Capacitor vào bridge đã dọn. Vẫn gọi super của Activity đầy đủ. */
    private Bridge suspendDisposedBridge() {
        Bridge old = bridge;
        if (bridgeDisposed) bridge = null;
        return old;
    }

    @Override
    public void onDestroy() {
        NativeDiag.log(this, "Màn hình app bị đóng" + (isFinishing() ? " (thoát / vuốt tắt)" : " (Android thu hồi màn hình)")
            + (playing ? " · đang phát" + (bgPlay ? " → giữ phát nền" : "") : ""));
        // Vuốt tắt app khi đang phát → giữ WebView sống để nhạc tiếp tục (dịch vụ nền giữ app không bị tắt)
        if (!rendererGone && !bridgeDisposed && playing && bgPlay && !isChangingConfigurations() && bridge != null && orphan == null) {
            orphan = this;
            // Giữ âm thanh nhưng không giữ view trong cửa sổ Activity đã đóng.
            try { WebView v = bridge.getWebView(); if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v); } catch (Exception ignored) {}
        }
        // Vuốt tắt / thoát app khi KHÔNG phát (đã tạm dừng) → tắt hẳn dịch vụ phát nền + bảng phát nhạc, không để app sót lại dưới nền
        else if (isFinishing() && !playing && !isChangingConfigurations()) {
            try { Class.forName("io.github.jofr.capacitor.mediasessionplugin.MediaSessionService").getMethod("dfKill").invoke(null); } catch (Exception ignored) {}
        }
        if (orphan != this) disposeBridge();
        Bridge old = bridge;
        bridge = null; // BridgeActivity không dọn lần hai
        super.onDestroy();
        if (orphan == this) bridge = old;
    }

    @Override
    public void onDetachedFromWindow() {
        // BridgeActivity luôn destroy WebView ở đây; owner đã dọn hoặc đang giữ nền.
        // Không gọi lần hai vào WebView đã destroy.
        if (bridge == null || bridgeDisposed || orphan == this) return;
        super.onDetachedFromWindow();
    }

    private void readAutoplay(Intent intent) {
        if (intent != null && intent.getBooleanExtra("dfAutoplay", false)) {
            pendingAutoplay = true;
            intent.removeExtra("dfAutoplay");
        }
    }

    @Override
    public void onStart() {
        Bridge old = suspendDisposedBridge();
        try { super.onStart(); } finally { bridge = old; }
    }

    @Override
    public void onResume() {
        Bridge old = suspendDisposedBridge();
        try { super.onResume(); } finally { bridge = old; }
        isForeground = true;
        if (rendererGone) scheduleRecreate();
        else updateRendererPriority();
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
    public void onPause() {
        isForeground = false;
        Bridge old = suspendDisposedBridge();
        try { super.onPause(); } finally { bridge = old; }
        keepWebViewAlive();
    }

    @Override
    public void onStop() {
        Bridge old = suspendDisposedBridge();
        try { super.onStop(); } finally { bridge = old; }
        // Tắt "phát dưới nền": rời app (không phải chỉ tắt màn hình) thì dừng phát
        if (!bridgeDisposed && !bgPlay && playing && !isChangingConfigurations()) {
            boolean screenOn = true;
            try { screenOn = ((PowerManager) getSystemService(POWER_SERVICE)).isInteractive(); } catch (Exception ignored) {}
            if (screenOn) {
                try { getBridge().getWebView().evaluateJavascript("try{audio.pause()}catch(e){}", null); } catch (Exception ignored) {}
            }
        }
        keepWebViewAlive();
    }

    void updateRendererPriority() {
        if (bridgeDisposed || rendererGone || bridge == null || Build.VERSION.SDK_INT < 26) return;
        runOnUiThread(() -> {
            if (bridgeDisposed || rendererGone || bridge == null) return;
            try { bridge.getWebView().setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, !playing); } catch (Exception ignored) {}
        });
    }

    /** Giữ JavaScript (hẹn giờ, lặp câu, chuyển bài) tiếp tục chạy khi app ở dưới nền */
    private void keepWebViewAlive() {
        if (rendererGone || bridgeDisposed || bridge == null || !playing || !bgPlay) return;
        try { getBridge().getWebView().onResume(); getBridge().getWebView().resumeTimers(); } catch (Exception ignored) {}
    }

    /** Ghi lỗi Java vào nhật ký lõi Android trước khi app bị tắt */
    static class DfCrashHandler implements Thread.UncaughtExceptionHandler {
        private final android.content.Context ctx; private final Thread.UncaughtExceptionHandler prev;
        DfCrashHandler(android.content.Context c, Thread.UncaughtExceptionHandler p) { ctx = c; prev = p; }
        @Override public void uncaughtException(Thread t, Throwable e) {
            try {
                for (String line : CrashDetails.describe(e)) NativeDiag.logNow(ctx, line);
            } catch (Throwable ignored) {}
            if (prev != null) prev.uncaughtException(t, e);
        }
    }
}
