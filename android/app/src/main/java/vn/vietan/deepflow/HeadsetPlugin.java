package vn.vietan.deepflow;

import android.Manifest;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

/** Báo cho app web biết khi tai nghe (dây hoặc Bluetooth) được kết nối. */
@CapacitorPlugin(
    name = "Headset",
    permissions = { @Permission(alias = "bt", strings = { Manifest.permission.BLUETOOTH_CONNECT }) }
)
public class HeadsetPlugin extends Plugin {
    static final String PREFS = "deepflow";
    private BroadcastReceiver receiver;
    /** Plugin đang chạy (để widget gửi lệnh vào app) */
    static volatile HeadsetPlugin inst = null;

    @Override
    public void load() {
        inst = this;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (isInitialStickyBroadcast()) return;
                String type = HeadsetReceiver.connectedType(intent);
                if (type != null) {
                    JSObject d = new JSObject();
                    d.put("type", type);
                    notifyListeners("connected", d);
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(AudioManager.ACTION_HEADSET_PLUG);
        f.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        f.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            getContext().registerReceiver(receiver, f, Context.RECEIVER_EXPORTED);
        } else {
            getContext().registerReceiver(receiver, f);
        }
    }

    /** Lưu chế độ phát hiện tại để thông báo phát nhạc hiện đúng biểu tượng nút chế độ */
    @PluginMethod
    public void setPlayMode(PluginCall call) {
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("pm", call.getString("pm", "ralb"))
            .putString("pmLabel", call.getString("label", "Chế độ phát"))
            .apply();
        call.resolve();
    }

    /** Chẩn đoán nút trên thông báo phát nhạc (đọc từ plugin media-session đã vá) */
    @PluginMethod
    public void getDiag(PluginCall call) {
        JSObject r = new JSObject();
        try {
            Class<?> c = Class.forName("io.github.jofr.capacitor.mediasessionplugin.MediaSessionService");
            r.put("diag", String.valueOf(c.getField("dfDiag").get(null)));
        } catch (Exception e) { r.put("diag", "lỗi: " + e); }
        call.resolve(r);
    }

    /** Nhật ký lõi Android + lý do Android kết thúc app các lần trước (Android 11+) */
    @PluginMethod
    public void getNativeDiag(PluginCall call) {
        JSObject r = new JSObject();
        try {
            r.put("log", NativeDiag.readLog(getContext()));
            r.put("exits", NativeDiag.exits(getContext()));
            r.put("sdk", Build.VERSION.SDK_INT);
            r.put("model", Build.MANUFACTURER + " " + Build.MODEL);
            try { Class<?> c = Class.forName("io.github.jofr.capacitor.mediasessionplugin.MediaSessionService"); r.put("fg", String.valueOf(c.getField("dfFg").get(null))); } catch (Exception e) { r.put("fg", "?"); }
        } catch (Exception e) { r.put("err", e.toString()); }
        call.resolve(r);
    }

    /** App web báo đang phát hay dừng (để giữ app chạy tiếp khi người dùng vuốt tắt app) */
    @PluginMethod
    public void setPlaying(PluginCall call) {
        MainActivity.playing = call.getBoolean("playing", false);
        MainActivity.bgPlay = call.getBoolean("bg", true);
        try { DfWidget.refresh(getContext()); } catch (Exception ignored) {}
        call.resolve();
    }

    @Override
    protected void handleOnDestroy() {
        if (inst == this) inst = null;
        try { getContext().unregisterReceiver(receiver); } catch (Exception ignored) {}
    }

    /** Widget màn hình chính bấm nút → báo cho app web */
    boolean fireWidget(String cmd) {
        try {
            if (getBridge() == null || getBridge().getWebView() == null) return false;
            JSObject d = new JSObject();
            d.put("cmd", cmd);
            notifyListeners("widget", d);
            return true;
        } catch (Exception e) { return false; }
    }

    /** App web gửi câu đang phát / tên bài / trạng thái phát cho widget. Trả về số widget đang đặt. */
    @PluginMethod
    public void setWidget(PluginCall call) {
        Context ctx = getContext();
        int n = DfWidget.ids(ctx).length;
        if (n > 0) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("wLine", call.getString("line", ""))
                .putString("wTitle", call.getString("title", ""))
                .putBoolean("wPlaying", Boolean.TRUE.equals(call.getBoolean("playing", false)))
                .putFloat("wPos", call.getFloat("pos", 0f))
                .putFloat("wDur", call.getFloat("dur", 0f))
                .putLong("wAt", System.currentTimeMillis())
                .apply();
            DfWidget.refresh(ctx);
        }
        JSObject r = new JSObject();
        r.put("n", n);
        call.resolve(r);
    }

    @PluginMethod
    public void setAutoLaunch(PluginCall call) {
        boolean on = Boolean.TRUE.equals(call.getBoolean("enabled", false));
        SharedPreferences p = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putBoolean("autoLaunch", on).apply();
        if (on && Build.VERSION.SDK_INT >= 31 && getPermissionState("bt") != PermissionState.GRANTED) {
            requestPermissionForAlias("bt", call, "btPermCallback");
            return;
        }
        call.resolve();
    }

    @PermissionCallback
    private void btPermCallback(PluginCall call) {
        JSObject r = new JSObject();
        r.put("bluetooth", getPermissionState("bt") == PermissionState.GRANTED);
        call.resolve(r);
    }

    /** App vừa được mở từ thông báo tai nghe? (chỉ trả về true một lần) */
    @PluginMethod
    public void consumeAutoplay(PluginCall call) {
        JSObject r = new JSObject();
        r.put("autoplay", MainActivity.pendingAutoplay);
        MainActivity.pendingAutoplay = false;
        call.resolve(r);
    }
}
