package vn.vietan.deepflow;

import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import vn.vietan.deepflow.study.*;

@CapacitorPlugin(name="StudySchedule")
public class StudySchedulePlugin extends Plugin {
    private static volatile StudySchedulePlugin instance;
    @Override public void load(){instance=this;}
    @Override protected void handleOnDestroy(){if(instance==this)instance=null;super.handleOnDestroy();}
    public static void emit(boolean active){StudySchedulePlugin p=instance;if(p!=null)p.getActivity().runOnUiThread(()->{JSObject r=new JSObject();r.put("active",active);p.notifyListeners("state",r);});}
    private static final long MAX=250L*1024*1024;
    @PluginMethod public void status(PluginCall call){
        getActivity().runOnUiThread(()->{
            try{JSObject r=new JSObject();r.put("supported",true);r.put("exact",StudyAlarms.exact(getContext()));
                NotificationManager nm=(NotificationManager)getContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE);
                r.put("notifications",Build.VERSION.SDK_INT<24||nm.areNotificationsEnabled());
                r.put("active",StudyPlaybackService.instance!=null);r.put("data",StudyAlarms.data(getContext()));
                r.put("reports",new JSONArray(StudyAlarms.prefs(getContext()).getString("reports","[]")));
                r.put("message",StudyAlarms.prefs(getContext()).getString("message",""));r.put("messageAt",StudyAlarms.prefs(getContext()).getLong("messageAt",0));
                call.resolve(r);
            }catch(Exception e){call.reject("Không đọc được lịch học.");}
        });
    }
    @PluginMethod public void permission(PluginCall call){
        if(Build.VERSION.SDK_INT>=31&&!StudyAlarms.exact(getContext())){
            try{getActivity().startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getContext().getPackageName())));}catch(Exception ignored){}
        }
        call.resolve();
    }
    @PluginMethod public void hasFile(PluginCall call){
        try{File f=StudyAlarms.file(getContext(),call.getString("id"));JSObject r=new JSObject();r.put("exists",f.isFile()&&f.length()==call.getLong("size",-1L));call.resolve(r);}
        catch(Exception e){call.reject("Không kiểm tra được âm thanh.");}
    }
    @PluginMethod public synchronized void chunk(PluginCall call){
        try{
            String id=call.getString("id"),encoded=call.getString("data","");long offset=call.getLong("offset",-1L),size=call.getLong("size",0L);
            if(offset<0||size<=0||size>100L*1024*1024||encoded.length()>750000)throw new IllegalArgumentException();
            File target=StudyAlarms.file(getContext(),id),dir=target.getParentFile();dir.mkdirs();File part=new File(dir,id+".part");
            byte[] bytes=android.util.Base64.decode(encoded,android.util.Base64.DEFAULT);
            if(bytes.length==0||offset+bytes.length>size)throw new IllegalArgumentException();
            long used=0;File[] files=dir.listFiles();if(files!=null)for(File f:files)used+=f.length();
            if(used+bytes.length>MAX)throw new IllegalStateException("Lịch học dùng tối đa 250 MB âm thanh riêng. Giảm số bài và lưu lại.");
            if(offset!=0&&(!part.exists()||part.length()!=offset))throw new IllegalArgumentException();
            try(FileOutputStream out=new FileOutputStream(part,offset>0)){out.write(bytes);}
            if(offset+bytes.length==size&&!part.renameTo(target))throw new IllegalStateException("Không lưu được âm thanh lịch học.");
            call.resolve();
        }catch(Exception e){call.reject(e instanceof IllegalStateException?e.getMessage():"Dữ liệu âm thanh lịch học không hợp lệ.");}
    }
    private void validateContext(JSONObject ctx)throws Exception{
        for(String key:new String[]{"main","learn","review"}){
            JSONArray q=ctx.getJSONArray(key);if(q.length()>100)throw new IllegalArgumentException();
            for(int i=0;i<q.length();i++){JSONObject t=q.getJSONObject(i);if(t.getString("id").length()>200||!StudyPolicy.id(t.getString("file"))||!StudyAlarms.file(getContext(),t.getString("file")).isFile())throw new IllegalArgumentException();}
        }
    }
    @PluginMethod public synchronized void save(PluginCall call){
        try{
            JSObject data=call.getObject("data");if(data==null)throw new IllegalArgumentException();
            JSONArray rows=data.getJSONArray("rows");if(rows.length()>24)throw new IllegalArgumentException();
            Set<String> times=new HashSet<>();
            for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);
                StudyPolicy.next(r.getString("time"),System.currentTimeMillis(),java.util.TimeZone.getDefault());
                if(!times.add(r.getString("time")))throw new IllegalArgumentException();
                if(!"main".equals(r.getString("kind"))&&!"review".equals(r.getString("kind")))throw new IllegalArgumentException();
                if(!r.getString("mode").matches("one|album|learn|r1|ralb|rlearn"))throw new IllegalArgumentException();
            }
            validateContext(data.getJSONObject("context"));
            if(!StudyAlarms.prefs(getContext()).edit().putString("data",data.toString()).commit())throw new IllegalStateException();
            StudyAlarms.schedule(getContext());cleanup(data.getJSONObject("context"));call.resolve();
        }catch(Exception e){call.reject("Chưa lưu được lịch. Kiểm tra giờ (không trùng), âm thanh và dung lượng trống.");}
    }
    @PluginMethod public synchronized void context(PluginCall call){
        try{JSObject ctx=call.getObject("context");validateContext(ctx);JSONObject data=StudyAlarms.data(getContext());data.put("context",ctx);
            StudyAlarms.prefs(getContext()).edit().putString("data",data.toString()).apply();call.resolve();
        }catch(Exception e){call.reject("Cần mở Lịch học và chuẩn bị lại âm thanh.");}
    }
    private void cleanup(JSONObject context)throws Exception{
        if(StudyPlaybackService.instance!=null)return;
        Set<String> keep=new HashSet<>();for(String key:new String[]{"main","learn","review"}){JSONArray q=context.getJSONArray(key);for(int i=0;i<q.length();i++)keep.add(q.getJSONObject(i).getString("file")+".audio");}
        File dir=new File(getContext().getFilesDir(),"study60");File[] files=dir.listFiles();if(files!=null)for(File f:files)if(!keep.contains(f.getName()))f.delete();
    }
    @PluginMethod public void stop(PluginCall call){
        getActivity().runOnUiThread(()->{StudyPlaybackService s=StudyPlaybackService.instance;if(s!=null)s.stopStudy("Đã dừng lịch học để chuyển thao tác.");call.resolve();});
    }
    @PluginMethod public void reschedule(PluginCall call){StudyAlarms.schedule(getContext());call.resolve();}
}
