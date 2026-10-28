package vn.vietan.deepflow.video;

import java.net.URI;
import java.util.Locale;

/** Quy tắc nguồn độc lập Android để kiểm thử URL mà không tải video. */
public final class VideoPolicy {
    private VideoPolicy() {}
    public static String remote(String raw) {
        try {
            if (raw == null || raw.length() > 4096) throw new IllegalArgumentException();
            URI u = new URI(raw.trim());
            String host = u.getHost(), path = u.getPath();
            if (!"https".equalsIgnoreCase(u.getScheme()) || host == null || u.getUserInfo() != null || u.getFragment() != null) throw new IllegalArgumentException();
            host = host.toLowerCase(Locale.ROOT);
            for (String blocked : new String[]{"youtube.com", "youtu.be", "youtube-nocookie.com", "vimeo.com"})
                if (host.equals(blocked) || host.endsWith("." + blocked)) throw new IllegalArgumentException();
            if (path == null || !path.toLowerCase(Locale.ROOT).matches(".*\\.(mp4|m4v|webm|m3u8)$")) throw new IllegalArgumentException();
            return u.toASCIIString();
        } catch (Exception e) { throw new IllegalArgumentException("Cần link HTTPS trực tiếp .mp4, .webm hoặc .m3u8; link trang YouTube/Vimeo chưa được hỗ trợ."); }
    }
    public static String error(int code) {
        if (code == 401 || code == 403) return "Không được phép truy cập video hoặc link đã hết hạn (403). Hãy lấy link mới từ nguồn.";
        if (code == 404) return "Không tìm thấy video (404). Kiểm tra lại nguồn.";
        if (code == 429) return "Nguồn video đang giới hạn tốc độ (429). Hãy chờ rồi bấm phát để thử lại; app không lặp vô hạn.";
        if (code >= 3000 && code < 5000) return "Thiết bị chưa giải mã được định dạng video/âm thanh này.";
        return "Chưa phát được video. Kiểm tra mạng, quyền truy cập, định dạng và thời hạn link; bấm phát để thử lại.";
    }
}
