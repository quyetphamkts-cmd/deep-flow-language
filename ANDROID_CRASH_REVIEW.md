# Phát nền / WebView — 28/10/2026

Log máy SM-S918B Android 16 cho thấy các nhóm độc lập:
- `LOW_MEMORY`: hệ thống ghi nhận thu hồi vì RAM; JS heap 10–28 MB không bao gồm bộ nhớ native/renderer.
- `JniAndroid$UncaughtExceptionException`: exception Chromium bọc lỗi Java; bản cũ lấy `toString()` trước, cắt 600 ký tự nên mất cause và Java frames. Hai `CRASH` 04:07 / 04:21 không có stack trong dữ liệu gửi, chưa thể khẳng định cùng nguyên nhân.
- `OTHER [ISOLATED NOT NEEDED]` ở tiến trình con WebView thường là dọn tiến trình phụ, không được gọi là app chính crash.
- Quota Gemini / tác vụ phụ đề hết lượt là lỗi API riêng, không chứng minh nguyên nhân process crash.

## Sửa
- Dọn bridge và WebView cũ đồng bộ trước khi tạo phiên mới, bỏ task dọn trễ 800ms gây chồng phiên; tháo view khỏi cửa sổ Activity đã đóng khi giữ phát nền.
- Renderer death chỉ xử lý một lần/owner, ngắt plugin/service và hủy view một lần; không resume timer hay gọi vòng đời play vào view đã chết; phục hồi giao diện khi foreground, không tự bật nhạc trong callback crash.
- Giới hạn tự dựng lại hai lần/phút; lỗi lặp cần thao tác người dùng. MediaSession bỏ late callback và action vào plugin đã destroy, kill đồng bộ trên UI thread.
- Không giải mã toàn bài để tạo sóng âm trên Android; vẫn dùng peaks đã có hoặc sóng tổng hợp. Web chỉ giải mã khi dừng/foreground, bài <=3 phút, <=8MB, tối đa một tác vụ. Hạn mức tránh compressed audio 80MB nở thành PCM hàng trăm MB.
- Ghi nguyên nhân Java sâu nhất và frames thành dòng riêng, che token/key, thêm phiên bản WebView vào exit log. PSS không được ghi nhận không hiện thành RAM 0MB. Dọn tiến trình phụ bình thường vào Hoạt động.

## Kiểm thử
Các kiểm thử Node/Chrome hiện có, thêm tình huống giới hạn giải mã và phân loại process. Java unit kiểm tra cause dài/cycle và che key; Android instrumentation dùng chrome://crash trên emulator API36 để kết thúc renderer thật, xác nhận host sống và Activity tạo lại. Kết quả chạy CI được cập nhật trong PR.

Không có thiết bị Samsung hoặc logcat đầy đủ. Emulator không tái hiện được mọi hạn chế pin/RAM của One UI, tai nghe thật hoặc native crash của đúng bản Android System WebView trên máy người dùng. Đây là sửa các lỗi vòng đời xác định từ mã nguồn và giảm tải; không khẳng định đã xác định được mọi crash trong log hay ngăn được mọi lần Android thu hồi app.

Cài APK mới bằng khóa cũ để nhận sửa Java; cập nhật HTML riêng không thay được lõi Android. Giữ dữ liệu, không gỡ ứng dụng.

## Log SM-S918B ngày 31/10/2026 — sửa beta.64 / lõi 1.0.2-beta.63 (31)

Stack 02:50 xác định `ForegroundServiceDidNotStartInTimeException` ở
`io.github.jofr.capacitor.mediasessionplugin.MediaSessionService`. Plugin gọi
`startForegroundService()` nhưng trước đây chỉ gọi `startForeground()` trong
`connectAndInitialize()`, sau callback bind với Activity. Callback bị chậm hoặc
bị bỏ qua khi plugin/WebView đã dispose sẽ để quá hạn foreground.

Bản vá postinstall nay đưa dịch vụ lên foreground ngay đầu `onStartCommand`,
trước cả nhánh nút tùy chỉnh. Tạo kênh và thông báo chuẩn bị khi chưa có metadata;
phiên kết nối thành công thay bằng thông báo MediaSession bình thường. Bỏ qua
media intent nếu session chưa tồn tại. Không dùng sticky restart cho dịch vụ
không có WebView. Nếu sau 15 giây vẫn chưa có plugin kết nối thì dừng dịch vụ;
hủy timer khi kết nối hoặc hủy dịch vụ. Nếu promotion thất bại thì ghi log và
dừng, không nuốt lỗi rồi để dịch vụ tiếp tục chờ quá hạn.

Kiểm thử Node kiểm tra thứ tự promotion trước mọi nhánh return/dispatch, kênh,
icon, kiểu mediaPlayback, xử lý lỗi, timer orphan và tính idempotent của patch.
Đã chạy thành công cùng kiểm thử phân loại exit/RAM. Chưa build APK hoặc thử
trên thiết bị Android trong môi trường này (chưa có Android SDK).

CRASH 04:57 không kèm stack nên chưa xác định có cùng nguyên nhân. LOW_MEMORY
05:12 và renderer bị hệ thống kết thúc là các sự kiện riêng; bản vá timeout
không bảo đảm ngăn được việc Android thu hồi bộ nhớ. Cần cài đè APK mới để nhận
sửa dịch vụ Java, cập nhật HTML riêng không đủ.
