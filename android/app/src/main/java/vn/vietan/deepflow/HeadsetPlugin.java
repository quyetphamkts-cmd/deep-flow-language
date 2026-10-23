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

    @Override
    public void load() {
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

    /** App web báo đang phát hay dừng (để giữ app chạy tiếp khi người dùng vuốt tắt app) */
    @PluginMethod
    public void setPlaying(PluginCall call) {
        MainActivity.playing = call.getBoolean("playing", false);
        MainActivity.bgPlay = call.getBoolean("bg", true);
        call.resolve();
    }

    @Override
    protected void handleOnDestroy() {
        try { getContext().unregisterReceiver(receiver); } catch (Exception ignored) {}
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
