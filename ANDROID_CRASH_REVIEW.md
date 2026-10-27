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
