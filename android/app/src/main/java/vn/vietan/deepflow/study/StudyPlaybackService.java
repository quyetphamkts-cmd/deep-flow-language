package vn.vietan.deepflow.study;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import vn.vietan.deepflow.MainActivity;
import vn.vietan.deepflow.R;

/** Phiên nghe hẹn giờ độc lập với WebView. Âm thanh chỉ lấy từ bản riêng đã chuẩn bị. */
@UnstableApi
public class StudyPlaybackService extends MediaSessionService {
    public static volatile StudyPlaybackService instance;
    private ExoPlayer player;
    private MediaSession session;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private JSONObject report;
    private JSONArray completed=new JSONArray();
    private String lastId="";
    private int seconds=0,limitSeconds=0,lastIndex=-1;
    private boolean launched=false;
    private final Runnable tick=new Runnable(){public void run(){
        if(player==null)return;
        if(!StudyAlarmReceiver.headphones(StudyPlaybackService.this)){stopStudy("Dừng lịch học: tai nghe đã ngắt.");return;}
        if(player.isPlaying()){
            seconds++;try{JSONObject totals=report.getJSONObject("seconds");String day=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.ROOT).format(new java.util.Date());String bucket=day+"|"+lastId;totals.put(bucket,totals.optInt(bucket)+1);}catch(Exception ignored){}
            if(limitSeconds>0&&seconds>=limitSeconds){stopStudy("Đã đủ thời gian hẹn học.");return;}
        }
        if(seconds%5==0)persist();handler.postDelayed(this,1000);
    }};
    @Override public void onCreate(){
        super.onCreate();instance=this;vn.vietan.deepflow.StudySchedulePlugin.emit(true);
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel("study60","Lịch học",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,60,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        startForeground(1001,new NotificationCompat.Builder(this,"study60").setSmallIcon(R.drawable.ic_stat_deepflow).setContentTitle("Deep Flow · Lịch học").setContentText("Đang chuẩn bị bài nghe…").setContentIntent(open).build());
        player=new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),true);
        player.setHandleAudioBecomingNoisy(true);player.setWakeMode(C.WAKE_MODE_LOCAL);
        session=new MediaSession.Builder(this,player).setSessionActivity(open).setCallback(new MediaSession.Callback(){
            @Override public MediaSession.ConnectionResult onConnect(MediaSession s,MediaSession.ControllerInfo c){
                if(!c.getPackageName().equals(getPackageName())&&!c.isTrusted())return MediaSession.ConnectionResult.reject();
                return MediaSession.ConnectionResult.accept(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS);
            }
        }).build();
        player.addListener(new Player.Listener(){
            @Override public void onMediaItemTransition(@Nullable MediaItem item,int reason){
                if(reason==Player.MEDIA_ITEM_TRANSITION_REASON_AUTO||reason==Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)completeLast();
                lastId=item==null?"":item.mediaId;lastIndex=player.getCurrentMediaItemIndex();persist();
            }
            @Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_ENDED){completeLast();stopStudy("Đã phát xong lịch học.");}}
            @Override public void onPlayerError(PlaybackException e){stopStudy("Không phát được âm thanh đã chuẩn bị; mở Lịch học để lưu lại.");}
            @Override public void onIsPlayingChanged(boolean playing){persist();}
        });
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        super.onStartCommand(intent,flags,startId);
        if(intent!=null&&intent.hasExtra("index")&&!launched){launched=true;launch(intent.getIntExtra("index",-1));}
        else if(!launched)stopStudy("Chưa có lịch học được chọn.");
        return START_NOT_STICKY;
    }
    private void launch(int index){
        try{
            if(!StudyAlarmReceiver.headphones(this))throw new IllegalArgumentException("Bỏ lượt hẹn: chưa kết nối tai nghe.");
            JSONObject data=StudyAlarms.data(this),row=data.getJSONArray("rows").getJSONObject(index),context=data.getJSONObject("context");
            boolean review="review".equals(row.optString("kind"));
            String mode=row.optString("mode","one");
            JSONArray queue=context.getJSONArray(review?"review":mode.endsWith("learn")?"learn":"main");
            String reviewKey="";
            String today=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.ROOT).format(new java.util.Date());
            String choice=today.equals(context.optString("webReviewChoiceDay"))?context.optString("webReviewChoice",""):"";
            if(choice.isEmpty()&&today.equals(StudyAlarms.prefs(this).getString("reviewChoiceDay","")))choice=StudyAlarms.prefs(this).getString("reviewChoiceKey","");
            if(review){
                long first=Long.MAX_VALUE;
                for(int i=0;i<queue.length();i++){
                    JSONObject t=queue.getJSONObject(i);if(!choice.isEmpty()&&!choice.equals(t.optString("group")))continue;long due=t.optLong("due",Long.MAX_VALUE);
                    if(StudyAlarms.prefs(this).getLong("reviewAt:"+t.getString("id"),0)>=t.optLong("completedAt"))due=Math.max(due,StudyAlarms.prefs(this).getLong("reviewDue:"+t.getString("id"),0));
                    if(due<first){first=due;reviewKey=t.getString("group");}
                }
                if(first>System.currentTimeMillis())throw new IllegalArgumentException("Chưa có bài ôn đến hạn.");
                JSONArray selected=new JSONArray();for(int i=0;i<queue.length();i++){
                    JSONObject t=queue.getJSONObject(i);long due=t.optLong("due",Long.MAX_VALUE);
                    if(StudyAlarms.prefs(this).getLong("reviewAt:"+t.getString("id"),0)>=t.optLong("completedAt"))due=Math.max(due,StudyAlarms.prefs(this).getLong("reviewDue:"+t.getString("id"),0));
                    if(t.getString("group").equals(reviewKey)&&due<=System.currentTimeMillis())selected.put(t);
                }queue=selected;
            }
            String day=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.ROOT).format(new java.util.Date());
            if(review&&day.equals(StudyAlarms.prefs(this).getString("reviewDay","")))throw new IllegalArgumentException("Đã hoàn thành lịch ôn hôm nay.");
            if(review&&day.equals(context.optString("webReviewDay")))throw new IllegalArgumentException("Đã hoàn thành lịch ôn hôm nay trong app.");
            String chosen=StudyAlarms.prefs(this).getString("reviewChoiceDay","");
            if(review&&day.equals(chosen)&&!reviewKey.equals(StudyAlarms.prefs(this).getString("reviewChoiceKey","")))throw new IllegalArgumentException("Tiếp tục mục ôn đã chọn hôm nay trong app trước khi chuyển mục khác.");
            int length=!review&&("one".equals(mode)||"r1".equals(mode))?Math.min(1,queue.length()):queue.length();
            if(length==0)throw new IllegalArgumentException("Chưa chuẩn bị âm thanh; mở app để cập nhật lịch.");
            boolean repeat=mode.startsWith("r");int rounds=repeat?StudyPolicy.count(row.optInt("count",1)):1;
            limitSeconds="time".equals(row.optString("limit"))?Math.max(60,Math.min(720*60,row.optInt("minutes",30)*60)):0;
            List<MediaItem> items=new ArrayList<>();
            for(int n=0;n<(limitSeconds>0&&repeat?1:rounds);n++)for(int i=0;i<length;i++){
                JSONObject t=queue.getJSONObject(i);java.io.File file=StudyAlarms.file(this,t.getString("file"));
                if(!file.isFile()||file.length()==0)throw new IllegalArgumentException("Âm thanh lịch học không còn trên máy; lưu lại lịch.");
                items.add(new MediaItem.Builder().setUri(android.net.Uri.fromFile(file)).setMediaId(t.getString("id")).setMediaMetadata(new MediaMetadata.Builder().setTitle(t.optString("title","Bài học")).setArtist("Deep Flow · Lịch học").build()).build());
            }
            report=new JSONObject().put("id",java.util.UUID.randomUUID().toString()).put("at",System.currentTimeMillis()).put("day",day).put("review",review).put("reviewKey",reviewKey).put("seconds",new JSONObject()).put("completed",completed).put("source",queue);
            if(review)StudyAlarms.prefs(this).edit().putString("reviewChoiceDay",day).putString("reviewChoiceKey",reviewKey).apply();
            player.setPlaybackSpeed((float)Math.max(.5,Math.min(2,row.optDouble("rate",1))));
            player.setMediaItems(items);if(limitSeconds>0&&repeat)player.setRepeatMode(Player.REPEAT_MODE_ALL);
            long pos=review?0:Math.max(0,context.optLong("position",0));
            String firstId=items.get(0).mediaId;
            if(!review&&StudyAlarms.prefs(this).getLong("positionAt:"+firstId,0)>context.optLong("updatedAt",0))pos=StudyAlarms.prefs(this).getLong("position:"+firstId,0);
            player.seekTo(0,pos);
            player.prepare();player.play();handler.postDelayed(tick,1000);
            StudyAlarms.log(this,"Đã bắt đầu "+(review?"ôn bài":"học tiếp")+" theo giờ hẹn.");
        }catch(Exception e){stopStudy(e instanceof IllegalArgumentException?e.getMessage():"Không đọc được lịch học; mở app để lưu lại.");}
    }
    private void completeLast(){
        if(lastId.isEmpty()||report==null)return;
        try{
            boolean first=true;for(int i=0;i<completed.length();i++)if(lastId.equals(completed.getJSONObject(i).optString("id")))first=false;
            long now=System.currentTimeMillis();
            if(!report.optBoolean("review"))StudyAlarms.prefs(this).edit().putLong("position:"+lastId,0).putLong("positionAt:"+lastId,now).apply();
            if(first){completed.put(new JSONObject().put("id",lastId).put("at",now));
                if(report.optBoolean("review")){
                    int step=0;JSONArray source=report.getJSONArray("source");for(int i=0;i<source.length();i++)if(source.getJSONObject(i).getString("id").equals(lastId))step=source.getJSONObject(i).optInt("step",0);
                    step=Math.min(6,Math.max(step,StudyAlarms.prefs(this).getInt("reviewStep:"+lastId,0))+1);
                    int[] days={1,3,7,14,30,60,120};StudyAlarms.prefs(this).edit().putLong("reviewAt:"+lastId,now).putLong("reviewDue:"+lastId,now+days[step]*86400000L).putInt("reviewStep:"+lastId,step).apply();
                }
            }
            // Mỗi mục chỉ cần một lần kết thúc để lên lịch ôn, dù người dùng chọn lặp nhiều lượt.
            if(completed.length()>200)completed.remove(0);
            if(report.optBoolean("review")&&completed.length()>=report.getJSONArray("source").length()){
                String day=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.ROOT).format(new java.util.Date());StudyAlarms.prefs(this).edit().putString("reviewDay",day).apply();report.put("reviewDone",true).put("reviewDay",day);
            }
        }catch(Exception ignored){}persist();
    }
    private void persist(){
        if(report==null||player==null)return;
        try{report.put("updatedAt",System.currentTimeMillis()).put("active",player.isPlaying());
            if(player.getCurrentMediaItem()!=null&&!lastId.isEmpty()){
                long pos=player.getPlaybackState()==Player.STATE_ENDED?0:Math.max(0,player.getCurrentPosition());
                report.put("position",pos).put("currentId",lastId);
                if(!report.optBoolean("review"))StudyAlarms.prefs(this).edit().putLong("position:"+lastId,pos).putLong("positionAt:"+lastId,System.currentTimeMillis()).apply();
            }
            JSONArray old=new JSONArray(StudyAlarms.prefs(this).getString("reports","[]")),out=new JSONArray();
            for(int i=0;i<old.length();i++)if(!old.getJSONObject(i).optString("id").equals(report.optString("id")))out.put(old.getJSONObject(i));
            out.put(report);while(out.length()>100)out.remove(0);
            StudyAlarms.prefs(this).edit().putString("reports",out.toString()).apply();
        }catch(Exception ignored){}
    }
    public void stopStudy(String reason){
        if(player!=null){player.pause();persist();player.stop();player.clearMediaItems();}handler.removeCallbacksAndMessages(null);
        StudyAlarms.log(this,reason);stopForeground(true);stopSelf();
    }
    @Override @Nullable public MediaSession onGetSession(MediaSession.ControllerInfo c){return session;}
    @Override public void onTaskRemoved(Intent root){if(player==null||!player.getPlayWhenReady())stopSelf();}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);persist();if(instance==this)instance=null;if(session!=null)session.release();if(player!=null)player.release();player=null;vn.vietan.deepflow.StudySchedulePlugin.emit(false);super.onDestroy();}
}
