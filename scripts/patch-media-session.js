// Thêm 2 nút vào thông báo / màn hình khoá của plugin @jofr/capacitor-media-session:
//  - "dfmode": nút chế độ phát (bên trái), biểu tượng theo chế độ đang chọn
//  - "dfclose": nút tắt (bên phải)
// Chạy tự động sau "npm install"/"npm ci" (postinstall). Chạy lại nhiều lần vẫn an toàn.
const fs = require('fs'), path = require('path');
const dir = path.join(__dirname, '..', 'node_modules', '@jofr', 'capacitor-media-session', 'android', 'src', 'main', 'java', 'io', 'github', 'jofr', 'capacitor', 'mediasessionplugin');
const svcF = path.join(dir, 'MediaSessionService.java'), cbF = path.join(dir, 'MediaSessionCallback.java');
if (!fs.existsSync(svcF)) { console.log('[patch-media-session] plugin not installed, skip'); process.exit(0); }
let svc = fs.readFileSync(svcF, 'utf8'), cb = fs.readFileSync(cbF, 'utf8');
function rep(src, a, b, name) { if (src.includes(b.trim().slice(0, 60)) && !src.includes(a)) return src; if (!src.includes(a)) throw new Error('[patch-media-session] không tìm thấy đoạn: ' + name); return src.replace(a, b); }
if (!svc.includes('DF_PATCH')) {
  svc = rep(svc, 'import android.content.Intent;', 'import android.content.Intent;\nimport android.content.SharedPreferences;\nimport android.os.Bundle; // DF_PATCH', 'imports');
  // nút tuỳ chỉnh từ thông báo (Android < 13) gửi về service
  svc = rep(svc, '    public int onStartCommand(Intent intent, int flags, int startId) {\n',
    '    public int onStartCommand(Intent intent, int flags, int startId) {\n' +
    '        if (intent != null && intent.getAction() != null && intent.getAction().startsWith("df.custom.") && plugin != null) {\n' +
    '            plugin.actionCallback(intent.getAction().substring(10));\n' +
    '            return START_NOT_STICKY;\n' +
    '        }\n', 'onStartCommand');
  // nút trên thông báo kiểu cũ: chế độ phát đầu tiên, tắt cuối cùng
  svc = rep(svc, '            int compactNotificationActionIndicesIndex = 0;\n',
    '            int compactNotificationActionIndicesIndex = 0;\n' +
    '            NotificationCompat.Action dfMode = dfAction("dfmode");\n' +
    '            if (dfMode != null && notificationBuilder != null) { notificationBuilder.addAction(dfMode); notificationActionIndex++; }\n', 'legacy mode action');
  svc = rep(svc, '            if (playbackStateBuilder != null) {\n              playbackStateBuilder.setActions(activePlaybackStateActions);',
    '            NotificationCompat.Action dfClose = dfAction("dfclose");\n' +
    '            if (dfClose != null && notificationBuilder != null) { notificationBuilder.addAction(dfClose); }\n' +
    '            // Android 13+: nút tuỳ chỉnh lấy từ PlaybackState (Builder không xoá được nút cũ → tạo mới)\n' +
    '            if (playbackStateBuilder != null) { playbackStateBuilder = new PlaybackStateCompat.Builder(); dfAddCustom(playbackStateBuilder); }\n' +
    '            if (playbackStateBuilder != null) {\n              playbackStateBuilder.setActions(activePlaybackStateActions);', 'custom actions');
  svc = rep(svc, '    public void updatePossibleActions() {',
    '    private int dfIcon(String name) {\n' +
    '        if (name.equals("dfclose")) return getResources().getIdentifier("ic_df_close", "drawable", getPackageName());\n' +
    '        SharedPreferences p = getSharedPreferences("deepflow", MODE_PRIVATE);\n' +
    '        return getResources().getIdentifier("ic_pm_" + p.getString("pm", "ralb"), "drawable", getPackageName());\n' +
    '    }\n' +
    '    private String dfLabel(String name) {\n' +
    '        if (name.equals("dfclose")) return "Tắt";\n' +
    '        return getSharedPreferences("deepflow", MODE_PRIVATE).getString("pmLabel", "Chế độ phát");\n' +
    '    }\n' +
    '    private void dfAddCustom(PlaybackStateCompat.Builder b) {\n' +
    '        try {\n' +
    '            for (String n : new String[]{"dfmode", "dfclose"}) {\n' +
    '                int ic = dfIcon(n);\n' +
    '                if (ic != 0 && plugin != null && plugin.hasActionHandler(n)) b.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(n, dfLabel(n), ic).build());\n' +
    '            }\n' +
    '        } catch (Exception ignored) {}\n' +
    '    }\n' +
    '    private NotificationCompat.Action dfAction(String n) {\n' +
    '        try {\n' +
    '            int ic = dfIcon(n);\n' +
    '            if (ic == 0 || plugin == null || !plugin.hasActionHandler(n)) return null;\n' +
    '            Intent i = new Intent(this, MediaSessionService.class).setAction("df.custom." + n);\n' +
    '            PendingIntent pi = PendingIntent.getService(this, n.hashCode(), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);\n' +
    '            return new NotificationCompat.Action(ic, dfLabel(n), pi);\n' +
    '        } catch (Exception e) { return null; }\n' +
    '    }\n\n' +
    '    public void updatePossibleActions() {', 'helpers');
  fs.writeFileSync(svcF, svc);
}
// dòng chữ phụ trên thông báo: không hiện " - " khi không có nghệ sĩ / album
if (!svc.includes('DF_TEXT')) {
  svc = rep(svc, '.setContentText(artist + " - " + album)', '.setContentText((artist.isEmpty() || album.isEmpty()) ? (artist + album) : (artist + " - " + album)) // DF_TEXT', 'content text');
  fs.writeFileSync(svcF, svc);
}
if (!cb.includes('onCustomAction')) {
  cb = rep(cb, '    @Override\n    public void onStop() {',
    '    @Override\n    public void onCustomAction(String action, android.os.Bundle extras) {\n        plugin.actionCallback(action);\n    }\n\n    @Override\n    public void onStop() {', 'onCustomAction');
  fs.writeFileSync(cbF, cb);
}
// thông báo chỉ giữ 5 nút: chế độ · bài trước · phát/dừng · bài sau · tắt (bỏ tua ±30s và nút dừng)
if (!svc.includes('DF_FIVE')) {
  svc = rep(svc, '                    if (notificationActions.containsKey(actionName)) {',
    '                    if (notificationActions.containsKey(actionName) && !actionName.startsWith("seek") && !actionName.equals("stop")) { // DF_FIVE', 'five actions');
  fs.writeFileSync(svcF, svc);
}
// thông tin chẩn đoán để app hiển thị trong Cài đặt
if (!svc.includes('dfDiag')) {
  svc = rep(svc, '    private void dfAddCustom(PlaybackStateCompat.Builder b) {\n        try {\n',
    '    public static volatile String dfDiag = "chưa khởi tạo";\n    private void dfAddCustom(PlaybackStateCompat.Builder b) {\n        try {\n' +
    '            dfDiag = "sdk=" + Build.VERSION.SDK_INT + " icMode=" + dfIcon("dfmode") + " icClose=" + dfIcon("dfclose") + " hMode=" + (plugin != null && plugin.hasActionHandler("dfmode")) + " hClose=" + (plugin != null && plugin.hasActionHandler("dfclose")) + " t=" + System.currentTimeMillis() / 1000;\n', 'diag');
  fs.writeFileSync(svcF, svc);
}
// mỗi chế độ phát dùng mã nút riêng để màn hình khoá (Samsung…) chịu cập nhật biểu tượng khi đổi chế độ
if (!svc.includes('DF_UID')) {
  svc = rep(svc, 'b.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(n, dfLabel(n), ic).build());',
    'b.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(n.equals("dfmode") ? n + ":" + getSharedPreferences("deepflow", MODE_PRIVATE).getString("pm", "ralb") : n, dfLabel(n), ic).build()); // DF_UID', 'unique id');
  fs.writeFileSync(svcF, svc);
}
cb = fs.readFileSync(cbF, 'utf8');
if (!cb.includes('DF_UID')) {
  cb = rep(cb, '    public void onCustomAction(String action, android.os.Bundle extras) {\n        plugin.actionCallback(action);',
    '    public void onCustomAction(String action, android.os.Bundle extras) {\n        plugin.actionCallback(action != null && action.startsWith("dfmode") ? "dfmode" : action); // DF_UID', 'callback map');
  fs.writeFileSync(cbF, cb);
}
// trạng thái "none" (bấm ✕ tắt): plugin gốc chỉ huỷ kết nối nên dịch vụ nền + thông báo vẫn còn, đồng hồ vẫn chạy → tắt hẳn dịch vụ
const plgF = path.join(dir, 'MediaSessionPlugin.java');
let plg = fs.readFileSync(plgF, 'utf8');
if (!plg.includes('DF_STOP')) {
  plg = rep(plg, '        } else if (startServiceOnlyDuringPlayback && service != null && !playback) {\n            getContext().unbindService(serviceConnection);',
    '        } else if (startServiceOnlyDuringPlayback && service != null && !playback) {\n            try { service.setPlaybackState(PlaybackStateCompat.STATE_STOPPED); service.update(); service.destroy(); } catch (Exception dfE) {} // DF_STOP\n            getContext().unbindService(serviceConnection);', 'stop service');
  fs.writeFileSync(plgF, plg);
}
console.log('[patch-media-session] OK');
