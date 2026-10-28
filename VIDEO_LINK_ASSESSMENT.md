# Đánh giá nhập link video — cập nhật 29/10/2026

Trạng thái: chỉ đánh giá sau khi phát hành beta.58, chưa thêm trình phát video, chưa gọi API trả phí hoặc đăng nhập tài khoản video. Beta.58 đã phát hành qua [PR #11](https://github.com/quyetphamkts-cmd/deep-flow-language/pull/11); tài liệu này không thay đổi bản app.

## Kết luận

Trải nghiệm **xem hình khi ở trong app, tiếp tục nghe khi ra nền/tắt màn hình, học qua phụ đề và bản dịch** khả thi với file/luồng được cấp quyền. Không thể coi mọi link trang xem video là link file có thể phát, tải phụ đề hoặc gửi cho AI. Chưa có thử nghiệm tài khoản/video thật để xác nhận từng nguồn.

| Nguồn | Xem hình trong app | Không quảng cáo của nền tảng | Nghe nền/tắt màn hình | Phụ đề riêng cho app |
|---|---|---|---|---|
| File/MP4/HLS được phép sử dụng | Khả thi, tùy codec và quyền truy cập | App có thể không chèn quảng cáo; nội dung nguồn vẫn có thể chứa quảng cáo | Khả thi bằng trình phát Android riêng | Nhập SRT/VTT hoặc nhận dạng từ âm thanh được cung cấp |
| Vimeo cho phép nhúng | Player SDK hỗ trợ | Vimeo công bố player nhúng không có quảng cáo nền tảng | Không coi iframe là bảo đảm phát nền | Có CC/SDK; tải transcript đầy đủ cần quyền phù hợp |
| Vimeo của chủ tài khoản có quyền lấy file | Có thể dùng đường phát chính thức được cấp | Phù hợp nhu cầu không chèn quảng cáo | Khả thi nếu API/nguồn cho phép dùng với trình phát riêng | API của chủ video hoặc ASR từ file được phép |
| Link YouTube công khai | IFrame API, nếu video cho nhúng | Không thể cam kết/chặn quảng cáo | Không đưa vào thiết kế API chính thức | Không có API tải transcript tùy ý của mọi video |

## YouTube: chỉ hỗ trợ một phần yêu cầu

[IFrame API](https://developers.google.com/youtube/iframe_api_reference) cho nhúng và điều khiển play/pause/seek/thời điểm, có thể đồng bộ phụ đề người dùng cung cấp. Video có thể bị chặn nhúng hoặc yêu cầu đăng nhập.

[Hướng dẫn Developer Policies](https://developers.google.com/youtube/terms/developer-policies-guide) không cho chặn/sửa quảng cáo, tách audio khỏi video và cho player phát khi cửa sổ ứng dụng đóng/thu nhỏ. Vì vậy không đề xuất hứa “YouTube không quảng cáo + nghe nền” trong Deep Flow. Tài khoản Premium không được coi là quyền tự động để app bên thứ ba bỏ các hạn chế API này.

[`captions.download`](https://developers.google.com/youtube/v3/docs/captions/download) cần OAuth và quyền chỉnh sửa video, không chỉ API key/link công khai. Nhìn thấy CC trong player không đồng nghĩa app tải được nội dung đó. Có thể dùng SRT/VTT được người sở hữu cung cấp; không thiết kế dựa vào endpoint transcript/giải mã URL không chính thức.

## Vimeo: phù hợp hơn, nhưng cần phân biệt nhúng và quyền lấy file

Vimeo mô tả [player nhúng không quảng cáo](https://vimeo.com/product/embeddable-video-player). Đây là quảng cáo do nền tảng chèn; lời tài trợ/quảng cáo đã nằm trong video vẫn là một phần nội dung. Quyền nhúng còn tùy cấu hình của video, mật khẩu, domain và URL unlisted.

[Player SDK](https://github.com/vimeo/player.js) điều khiển thời điểm, text track và sự kiện cue; chưa kiểm chứng đầy đủ việc lấy toàn bộ mốc/câu để dịch và tra từ trong WebView của app. Không coi `getTextTracks()` là API xuất mọi transcript.

Tài liệu [lấy đường file bằng API](https://help.vimeo.com/hc/en-us/articles/12427806914577-About-video-file-download-links-from-the-API) áp dụng cho video của tài khoản có gói đủ điều kiện và token đúng scope (`public`, `private`, `video_files`). Vimeo khuyến nghị trường `play` cho player bên thứ ba; URL có thời hạn nên cần làm mới khi cần, không lưu URL CDN cuối như địa chỉ vĩnh viễn. Không thể suy ra quyền này từ một link xem công khai.

[Tải transcript qua API](https://help.vimeo.com/hc/en-us/articles/17480150130833-How-to-access-and-download-video-transcripts-via-API) yêu cầu personal token của chủ tài khoản sở hữu video. Chưa thử OAuth, tài khoản có gói phù hợp hoặc video cụ thể; chưa cam kết hỗ trợ mọi link Vimeo.

## Đối chiếu kiến trúc Deep Flow

- `www/index.html`: trình phát chính hiện là `Audio`; nhận MP4/WebM như nguồn âm thanh, chưa có trình xem video tích hợp. Có PCM/ASR (`gemTranscribe`, `oaTranscribe`), dịch, cache/hàng đợi và thông báo quota có thể tái sử dụng khi đã có âm thanh hợp lệ.
- `MainActivity.java` đang cố giữ WebView để phát nền; `@jofr/capacitor-media-session` có dịch vụ điều khiển/thông báo, chưa phải engine video Media3 độc lập. Ẩn một iframe không biến nó thành trình phát nền bền vững. Điều này đặc biệt quan trọng vì thiết bị đã báo renderer bị hệ thống đóng.
- Đề xuất player mới dùng **Media3 ExoPlayer trong MediaSessionService**, còn giao diện gắn/tháo mặt hiển thị video. Cùng một player giữ vị trí khi chuyển xem/nghe; không tạo hai player phát chồng. Theo [Android](https://developer.android.com/media/media3/session/background-playback), player/session trong service hỗ trợ phát khi Activity không ở trước, điều khiển từ tai nghe và thông báo. Cần tích hợp với session hiện có để tránh hai bảng điều khiển tranh nhau.
- Khi tắt màn hình: bỏ render/giải mã hình khi phù hợp, chọn rendition chỉ audio nếu nguồn cung cấp. MP4 ghép chung hình/tiếng vẫn có thể phải tải cả dữ liệu video; không hứa tự giảm băng thông chỉ vì ẩn hình. Khi mở lại: gắn mặt hiển thị và trở về đúng vị trí, không tải lại bài từ đầu.
- “Ra nền/vuốt khỏi recent” khác **Force stop**: không hứa phát tiếp khi người dùng buộc dừng ứng dụng hoặc Android kết thúc tiến trình. Có thể lưu vị trí và phục hồi sau. Cần tôn trọng audio focus, cuộc gọi, tháo tai nghe, chế độ pin và nút dừng rõ ràng.
- Phần native này cần **APK mới, tăng versionCode, ký bằng khóa cũ**. Chỉ OTA file HTML như beta.58 chưa đủ cho kiến trúc đề xuất.

## Phụ đề và bản dịch

1. Ưu tiên file SRT/VTT gốc được cấp quyền, giữ mốc thời gian/nguồn/ngôn ngữ. Hiển thị rõ “phụ đề nguồn” khác “AI tạo”.
2. Không có phụ đề nhưng có âm thanh được phép xử lý: người dùng bấm tạo phụ đề, app chia đoạn có giới hạn RAM, nhận dạng rồi dịch và căn theo timeline thật. Không giải mã cả video dài vào RAM WebView.
3. Cache theo nguồn/phiên bản/nội dung/ngôn ngữ/model; gộp yêu cầu trùng, giới hạn công việc đồng thời và retry. Quota/số dư phải báo đúng, cho hủy và tiếp tục công việc dở. Không gửi âm thanh tới API trả phí trong giai đoạn đánh giá.
4. Chỉ có link iframe và không lấy được file/track: báo khả năng thực tế, cho nhập SRT/VTT hoặc mở nguồn. AI không tự có quyền truy cập âm thanh chỉ từ link đó. Dịch/ASR có thể sai do giọng, nhạc nền, chất lượng và mạng; không hứa đúng tuyệt đối.

## Bước thử nghiệm nên làm tiếp

Ưu tiên **file/link MP4/HLS được cấp quyền + SRT/VTT**, sau đó Vimeo của tài khoản có quyền lấy file. YouTube chỉ xem nhúng theo giới hạn nguồn; không đưa chế độ bỏ quảng cáo/nghe nền vào phạm vi này.

Trước khi phát hành video: thử Android 16 và Samsung thật, mở màn hình → Home → khóa màn hình → mở lại; giữ vị trí/phụ đề, tai nghe/cuộc gọi/notification, chuyển Wi-Fi/di động, URL hết hạn và video dài. Kiểm tra codec, RAM, pin, hủy ASR, lỗi 403/429, không phát chồng với bài nghe/flashcard và không làm thay đổi bài học chính/lịch ôn hiện có.

Đây là đánh giá và hướng triển khai, chưa phải chức năng đã chạy. Chưa có chi phí, benchmark hay thử nghiệm video/tài khoản thật. Chưa yêu cầu thêm khóa/token vì giai đoạn đọc tài liệu không cần chúng.

## Facebook trong đánh giá trước

Facebook/Reels cần nghiên cứu riêng nguồn công khai, nguồn của chủ tài khoản và video cần đăng nhập. Chưa xác minh đầy đủ API/quyền; kết luận YouTube/Vimeo không được áp dụng tự động cho Facebook. Tài liệu cần đối chiếu: [Embedded Video Player](https://developers.facebook.com/docs/plugins/embedded-video-player/), [video captions](https://developers.facebook.com/docs/graph-api/reference/video/captions/).
