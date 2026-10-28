package vn.vietan.deepflow.video;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import java.security.MessageDigest;

/** Player sở hữu bởi service, không phụ thuộc vòng đời WebView hoặc Activity. */
@UnstableApi
public class VideoPlaybackService extends MediaSessionService {
    public static volatile VideoPlaybackService instance;
    private ExoPlayer player;
    private MediaSession session;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saveTick = new Runnable() { public void run() { savePosition(); handler.postDelayed(this, 5000); } };
    public static String key(String uri) {
        try { byte[] a = MessageDigest.getInstance("SHA-256").digest(uri.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder(); for (byte n : a) b.append(String.format("%02x", n & 255)); return b.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    @Override public void onCreate() {
        super.onCreate(); instance = this;
        player = new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(this).setLoadErrorHandlingPolicy(new DefaultLoadErrorHandlingPolicy(0))).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
        player.setHandleAudioBecomingNoisy(true);
        player.setWakeMode(C.WAKE_MODE_LOCAL);
        PendingIntent open = PendingIntent.getActivity(this, 59, new Intent(this, VideoActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        session = new MediaSession.Builder(this, player).setSessionActivity(open).setCallback(new MediaSession.Callback() {
            @Override public MediaSession.ConnectionResult onConnect(MediaSession s, MediaSession.ControllerInfo c) {
                if (!c.getPackageName().equals(getPackageName()) && !c.isTrusted()) return MediaSession.ConnectionResult.reject();
                return MediaSession.ConnectionResult.accept(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS, MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS);
            }
        }).build();
        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean playing) { savePosition(); }
            @Override public void onPlayerError(PlaybackException e) { savePosition(); player.pause(); }
            @Override public void onMediaItemTransition(@Nullable MediaItem item, int reason) {
                // Nội dung được ghi riêng trên thiết bị, không đưa URL có token vào log.
                if (item != null && item.localConfiguration != null) {
                    SharedPreferences.Editor edit = getSharedPreferences("video_lessons", MODE_PRIVATE).edit();
                    edit.putString("lastUri", item.localConfiguration.uri.toString());
                    edit.putString("lastTitle", String.valueOf(item.mediaMetadata.title)); edit.apply();
                }
            }
        });
        handler.post(saveTick);
    }
    @Override @Nullable public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) { return session; }
    public boolean active() { return player != null && player.getMediaItemCount() > 0; }
    public void pauseVideo() { if (player != null) { player.pause(); savePosition(); } }
    public void videoVisible(boolean visible) {
        if (player != null) player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !visible).build());
    }
    private void savePosition() {
        if (player == null || player.getCurrentMediaItem() == null || player.getCurrentMediaItem().localConfiguration == null) return;
        String uri = player.getCurrentMediaItem().localConfiguration.uri.toString();
        getSharedPreferences("video_lessons", MODE_PRIVATE).edit().putLong("pos:" + key(uri), Math.max(0, player.getCurrentPosition())).apply();
    }
    @Override public void onTaskRemoved(Intent rootIntent) { if (player == null || !player.getPlayWhenReady()) stopSelf(); }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null); savePosition();
        if (instance == this) instance = null;
        if (session != null) session.release(); if (player != null) player.release();
        session = null; player = null; super.onDestroy();
    }
}
