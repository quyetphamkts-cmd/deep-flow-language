package vn.vietan.deepflow.video;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.media3.ui.PlayerView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;

/** Giao diện chỉ gắn vào player của service; Home/khóa màn hình không tạo player thứ hai. */
@UnstableApi
public class VideoActivity extends AppCompatActivity {
    private static final int PICK_SUBTITLE = 59;
    private MediaController controller;
    private ListenableFuture<MediaController> future;
    private PlayerView video;
    private TextView message;
    private String uri, title;
    private boolean firstOpen=true, audioOnly=false, started=false;
    private final Player.Listener listener=new Player.Listener() {
        @Override public void onIsPlayingChanged(boolean playing) { screen(); }
        @Override public void onPlaybackStateChanged(int state) { if(state==Player.STATE_READY) message.setText("Phụ đề: CC hoặc nhập SRT/VTT · Home/tắt màn hình để nghe nền"); screen(); }
        @Override public void onPlayerError(PlaybackException e) {
            int http=0; for(Throwable c=e;c!=null;c=c.getCause()) if(c instanceof HttpDataSource.InvalidResponseCodeException){http=((HttpDataSource.InvalidResponseCodeException)c).responseCode;break;}
            message.setText(VideoPolicy.error(http>0?http:e.errorCode)); screen();
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        uri=getIntent().getStringExtra("uri"); title=getIntent().getStringExtra("title");
        if(uri==null) { uri=getSharedPreferences("video_lessons",MODE_PRIVATE).getString("lastUri",null); title=getSharedPreferences("video_lessons",MODE_PRIVATE).getString("lastTitle","Video luyện nghe"); }
        if(uri==null){finish();return;}
        if(state!=null){firstOpen=false;audioOnly=state.getBoolean("audioOnly",false);}
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff101820);
        TextView heading=new TextView(this);heading.setText(title==null?"Video luyện nghe":title);heading.setTextColor(0xffeeeeee);heading.setTextSize(18);heading.setPadding(16,16,16,12);root.addView(heading);
        video=new PlayerView(this);video.setShowSubtitleButton(true);root.addView(video,new LinearLayout.LayoutParams(-1,0,1));
        message=new TextView(this);message.setText("Đang mở video…");message.setTextColor(0xffdedede);message.setPadding(16,8,16,8);root.addView(message);
        HorizontalScrollView scroll=new HorizontalScrollView(this);LinearLayout buttons=new LinearLayout(this);scroll.addView(buttons);root.addView(scroll);
        Button close=button(buttons,"Dừng / Đóng");close.setOnClickListener(v->closeVideo());
        Button background=button(buttons,"Nghe nền");background.setOnClickListener(v->{if(controller!=null){VideoPlaybackService s=VideoPlaybackService.instance;if(s!=null)s.videoVisible(false);finish();}});
        Button audio=button(buttons,"Chỉ nghe");audio.setOnClickListener(v->{audioOnly=!audioOnly;audio.setText(audioOnly?"Xem hình":"Chỉ nghe");updateVideo();});
        Button subtitles=button(buttons,"Phụ đề SRT/VTT");subtitles.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_SUBTITLE);});
        setContentView(root);
    }
    private Button button(LinearLayout parent,String text){Button b=new Button(this);b.setText(text);parent.addView(b);return b;}
    private MediaItem item() {
        MediaItem.Builder b=new MediaItem.Builder().setUri(uri).setMediaId(VideoPlaybackService.key(uri)).setMediaMetadata(new MediaMetadata.Builder().setTitle(title).build());
        if(Uri.parse(uri).getPath()!=null&&Uri.parse(uri).getPath().toLowerCase(java.util.Locale.ROOT).endsWith(".m3u8")) b.setMimeType(MimeTypes.APPLICATION_M3U8);
        String sub=getSharedPreferences("video_lessons",MODE_PRIVATE).getString("sub:"+VideoPlaybackService.key(uri),null);
        if(sub!=null){String mime=getSharedPreferences("video_lessons",MODE_PRIVATE).getString("submime:"+VideoPlaybackService.key(uri),MimeTypes.TEXT_VTT);
            b.setSubtitleConfigurations(Collections.singletonList(new MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub)).setMimeType(mime).setLabel("Phụ đề đã nhập").setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()));}
        return b.build();
    }
    @Override public void onStart(){super.onStart();started=true;if(uri==null)return;
        SessionToken token=new SessionToken(this,new ComponentName(this,VideoPlaybackService.class));
        final ListenableFuture<MediaController> pending=new MediaController.Builder(this,token).buildAsync();future=pending;
        pending.addListener(()->{if(!started||future!=pending)return;
            try {controller=pending.get();controller.addListener(listener);video.setPlayer(controller);
                MediaItem current=controller.getCurrentMediaItem();
                if(current==null||!current.mediaId.equals(VideoPlaybackService.key(uri))){long pos=getSharedPreferences("video_lessons",MODE_PRIVATE).getLong("pos:"+VideoPlaybackService.key(uri),0);controller.setMediaItem(item(),Math.max(0,pos));controller.prepare();}
                updateVideo();if(firstOpen){firstOpen=false;controller.play();}screen();
            } catch(Exception e){message.setText("Chưa kết nối được trình phát. Đóng rồi mở lại video.");}
        },ContextCompat.getMainExecutor(this));
    }
    private void updateVideo(){VideoPlaybackService s=VideoPlaybackService.instance;if(s!=null)s.videoVisible(started&&!audioOnly);if(video!=null)video.setVisibility(audioOnly?View.INVISIBLE:View.VISIBLE);screen();}
    private void screen(){if(controller!=null&&controller.isPlaying()&&!audioOnly&&started)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    private void closeVideo(){if(controller!=null){controller.pause();controller.stop();controller.clearMediaItems();}finish();}
    @Override public void onBackPressed(){closeVideo();}
    @Override public void onStop(){started=false;VideoPlaybackService s=VideoPlaybackService.instance;if(s!=null)s.videoVisible(false);if(video!=null)video.setPlayer(null);if(controller!=null)controller.removeListener(listener);controller=null;if(future!=null){MediaController.releaseFuture(future);future=null;}screen();super.onStop();}
    @Override protected void onSaveInstanceState(Bundle out){out.putBoolean("audioOnly",audioOnly);super.onSaveInstanceState(out);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK_SUBTITLE||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri sub=data.getData();
        // Đọc có giới hạn để không nạp nhầm file video vào RAM khi chọn phụ đề.
        try(InputStream in=getContentResolver().openInputStream(sub);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new Exception();byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1){if(out.size()+n>2*1024*1024)throw new IllegalArgumentException("Phụ đề quá lớn (tối đa 2 MB).");out.write(buf,0,n);}
            String text=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8).replace("\uFEFF","");
            String mime=text.trim().startsWith("WEBVTT")?MimeTypes.TEXT_VTT:MimeTypes.APPLICATION_SUBRIP;
            if(!text.contains("-->"))throw new IllegalArgumentException("Cần file SRT/VTT có mốc thời gian.");
            // Sao chép riêng file phụ đề nhỏ để không lệ thuộc quyền tạm của trình chọn file.
            java.io.File file=new java.io.File(getFilesDir(),"video-sub-"+VideoPlaybackService.key(uri)+(mime.equals(MimeTypes.TEXT_VTT)?".vtt":".srt"));
            try(java.io.FileOutputStream f=new java.io.FileOutputStream(file)){f.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            getSharedPreferences("video_lessons",MODE_PRIVATE).edit().putString("sub:"+VideoPlaybackService.key(uri),Uri.fromFile(file).toString()).putString("submime:"+VideoPlaybackService.key(uri),mime).apply();
            if(controller!=null){long pos=controller.getCurrentPosition();boolean play=controller.getPlayWhenReady();controller.setMediaItem(item(),pos);controller.prepare();controller.setPlayWhenReady(play);}
            message.setText("Đã nhập phụ đề. File song ngữ sẽ hiển thị theo nội dung đã cung cấp.");
        }catch(Exception e){message.setText(e instanceof IllegalArgumentException?e.getMessage():"Không đọc được phụ đề. Hãy chọn lại file SRT/VTT.");}
    }
}
