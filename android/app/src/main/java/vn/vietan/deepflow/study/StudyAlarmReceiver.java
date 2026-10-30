package vn.vietan.deepflow.study;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;

public class StudyAlarmReceiver extends BroadcastReceiver {
    public static boolean headphones(Context c) {
        try { AudioManager m=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
            if(m!=null)for(AudioDeviceInfo d:m.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                int t=d.getType(); if(t==AudioDeviceInfo.TYPE_WIRED_HEADPHONES || t==AudioDeviceInfo.TYPE_WIRED_HEADSET || t==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || t==AudioDeviceInfo.TYPE_USB_HEADSET || (Build.VERSION.SDK_INT>=31&&t==AudioDeviceInfo.TYPE_BLE_HEADSET))return true;
            }
        }catch(Exception ignored){} return false;
    }
    @Override public void onReceive(Context c, Intent intent) {
        if(!"vn.vietan.deepflow.STUDY60".equals(intent.getAction())) { StudyAlarms.schedule(c); return; }
        int index=intent.getIntExtra("index",-1); long at=intent.getLongExtra("at",0),now=System.currentTimeMillis();
        JSONObject data=StudyAlarms.data(c); JSONObject row=data.optJSONArray("rows")==null?null:data.optJSONArray("rows").optJSONObject(index);
        if(row==null||!row.optBoolean("enabled",true))return;
        if(at==StudyAlarms.prefs(c).getLong("fired:"+index,-1))return;
        StudyAlarms.prefs(c).edit().putLong("fired:"+index,at).apply(); StudyAlarms.schedule(c);
        if(at<=0 || now<at || now-at>10*60*1000){StudyAlarms.log(c,"Bỏ lượt hẹn đến quá muộn; giữ lịch lần sau.");return;}
        if(!headphones(c)){StudyAlarms.log(c,"Bỏ lượt hẹn: chưa kết nối tai nghe.");return;}
        if(StudyPlaybackService.instance!=null || vn.vietan.deepflow.MainActivity.playing){StudyAlarms.log(c,"Bỏ lượt hẹn: đang có phiên nghe, không phát chồng.");return;}
        try { ContextCompat.startForegroundService(c,new Intent(c,StudyPlaybackService.class).putExtra("index",index)); }
        catch(Exception e){StudyAlarms.log(c,"Android chặn tự phát nền. Mở lại app và kiểm tra quyền báo thức/pin.");}
    }
}
