package vn.vietan.deepflow;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import vn.vietan.deepflow.video.VideoActivity;
import vn.vietan.deepflow.video.VideoPlaybackService;
import vn.vietan.deepflow.video.VideoPolicy;

@CapacitorPlugin(name="VideoLessons")
public class VideoPlugin extends Plugin {
    @PluginMethod public void status(PluginCall call) {
        getActivity().runOnUiThread(() -> { JSObject r = new JSObject(); r.put("supported", true); r.put("active", VideoPlaybackService.instance != null && VideoPlaybackService.instance.active()); call.resolve(r); });
    }
    @PluginMethod public void pause(PluginCall call) {
        getActivity().runOnUiThread(() -> { if (VideoPlaybackService.instance != null) VideoPlaybackService.instance.pauseVideo(); call.resolve(); });
    }
    @PluginMethod public void history(PluginCall call) {
        JSObject r=new JSObject();try {r.put("items",new org.json.JSONArray(getContext().getSharedPreferences("video_lessons", Context.MODE_PRIVATE).getString("history","[]")));} catch(Exception e){r.put("items",new org.json.JSONArray());}call.resolve(r);
    }
    @PluginMethod public void pick(PluginCall call) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("video/*").addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(call, i, "picked");
    }
    @ActivityCallback private void picked(PluginCall call, ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) { JSObject r=new JSObject(); r.put("cancelled", true); call.resolve(r); return; }
        Uri uri = result.getData().getData(); boolean persisted = false;
        try { getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); persisted = true; } catch (Exception ignored) {}
        getContext().getSharedPreferences("video_lessons", Context.MODE_PRIVATE).edit().putBoolean("picked:" + VideoPlaybackService.key(uri.toString()), true).apply();
        String title = "Video trên máy";
        try (android.database.Cursor c=getContext().getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)) { if(c!=null&&c.moveToFirst()) title=c.getString(0); } catch(Exception ignored) {}
        JSObject r=new JSObject(); r.put("uri", uri.toString()); r.put("title", title); r.put("persisted", persisted); call.resolve(r);
    }
    @PluginMethod public void open(PluginCall call) {
        try {
            String raw = call.getString("uri", ""), title=call.getString("title", "Video luyện nghe"); Uri uri=Uri.parse(raw);
            if ("content".equals(uri.getScheme())) {
                if (!getContext().getSharedPreferences("video_lessons", Context.MODE_PRIVATE).getBoolean("picked:"+VideoPlaybackService.key(raw),false)) throw new IllegalArgumentException("Hãy chọn file bằng nút Video trên máy.");
                try (android.os.ParcelFileDescriptor fd=getContext().getContentResolver().openFileDescriptor(uri,"r")) { if(fd==null) throw new Exception(); }
                catch(Exception e) { throw new IllegalArgumentException("Không còn quyền đọc file. Hãy chọn lại video trên máy."); }
            } else raw=VideoPolicy.remote(raw);
            String caption=call.getString("subtitleText","");
            if(caption.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>2*1024*1024)throw new IllegalArgumentException("Phụ đề quá lớn (tối đa 2 MB).");
            if(!caption.isEmpty()){
                if(!caption.contains("-->"))throw new IllegalArgumentException("Phụ đề cần có mốc thời gian.");
                java.io.File f=new java.io.File(getContext().getFilesDir(),"video-sub-"+VideoPlaybackService.key(raw)+".srt");
                try(java.io.FileOutputStream out=new java.io.FileOutputStream(f)){out.write(caption.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                getContext().getSharedPreferences("video_lessons",Context.MODE_PRIVATE).edit().putString("sub:"+VideoPlaybackService.key(raw),Uri.fromFile(f).toString()).putString("submime:"+VideoPlaybackService.key(raw),"application/x-subrip").apply();
            }
            Intent intent=new Intent(getContext(), VideoActivity.class).putExtra("reloadSubtitle",!caption.isEmpty()).putExtra("uri",raw).putExtra("title",title.substring(0,Math.min(200,title.length())));
            org.json.JSONArray history=new org.json.JSONArray();
            org.json.JSONObject row=new org.json.JSONObject().put("uri",raw).put("title",title.substring(0,Math.min(200,title.length())));for(String k:new String[]{"topic","creator","license","sourcePage"}){String v=call.getString(k,"");row.put(k,v.substring(0,Math.min(k.equals("sourcePage")?4096:300,v.length())));}history.put(row);
            getContext().getSharedPreferences("video_lessons",Context.MODE_PRIVATE).edit().putString("meta:"+VideoPlaybackService.key(raw),row.toString()).apply();
            org.json.JSONArray previous=new org.json.JSONArray(getContext().getSharedPreferences("video_lessons",Context.MODE_PRIVATE).getString("history","[]"));
            for(int i=0;i<previous.length()&&history.length()<20;i++){org.json.JSONObject old=previous.optJSONObject(i);if(old!=null&&!raw.equals(old.optString("uri")))history.put(old);}
            getContext().getSharedPreferences("video_lessons",Context.MODE_PRIVATE).edit().putString("history",history.toString()).apply();
            getActivity().startActivity(intent); call.resolve();
        } catch(Exception e) { call.reject(e instanceof IllegalArgumentException ? e.getMessage() : "Chưa mở được video. Hãy chọn lại nguồn."); }
    }
}
