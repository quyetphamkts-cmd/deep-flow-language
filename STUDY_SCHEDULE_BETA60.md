# Deep Flow beta.60: giao diện, ôn tập và giờ hẹn

## Phạm vi

- Tạm ẩn nút Video và Giáo trình; giữ mã và dữ liệu, không xóa bài hoặc lịch sử.
- Ô Từ điển khai báo `lang=en`, không tự sửa hoặc viết hoa. Đây là gợi ý bàn phím; Android/IME vẫn cho phép người dùng chuyển ngôn ngữ, không cam kết ép bàn phím.
- Nhật ký nằm trong Phiên bản (mở rộng), hỗ trợ cấu hình tab nhật ký cũ.
- Lịch ôn lấy bài đã đánh dấu học xong, nhóm theo thư mục như trước. Hàng đợi mở rộng hoặc thu gọn còn một mục; ưu tiên hạn ôn, sau đó thời điểm học xong. Giữ mốc 1–3–7–14–30–60–120 ngày và giới hạn một mục/ngày. Ngày hiển thị dồn lịch tối đa một mục/ngày; đây là lịch gợi ý theo spaced repetition, không đo đường cong quên cá nhân.

## Huân chương giờ học

Đổi Chiến sĩ Bền bỉ thành **Chiến sĩ thao luyện**. Năm mốc: **1.000 – 2.000 – 4.000 – 6.000 – 10.000 giờ**. Giữ toàn bộ giờ học thực tế và bản ghi trao thưởng cũ; dùng con trỏ thông báo riêng cho mốc mới, không phát lại thông báo thưởng khi nâng cấp. Đây là hành trình thành tích, không phải quy đổi CEFR hoặc cam kết cứ đủ giờ sẽ thành thạo. Quân hàm theo họ từ giữ nguyên.

## Lịch học Android

Nút đồng hồ ở Nghe giữ hẹn giờ tắt cũ, thêm lịch học/ôn nhiều lần mỗi ngày. Tối đa 24 giờ khác nhau. Mỗi giờ chọn học tiếp hoặc ôn, chế độ bài/thư mục/đang học và các chế độ lặp tương ứng, số lượt 1–99 hoặc thời gian 1–720 phút. Chế độ không lặp vẫn dừng khi hết phạm vi.

Bấm Thêm giờ chỉ sửa bản nháp. **Lưu lịch** mới chuẩn bị âm thanh và ghi cấu hình Android. Cần APK mới; OTA trên APK cũ vẫn giữ phát bài bình thường nhưng chưa hỗ trợ hẹn nền.

Âm thanh gốc nằm trong IndexedDB/WebView, nên phiên hẹn độc lập cần bản riêng trong vùng private Android. Chỉ sao chép âm thanh đã có local, không tự tải khóa học từ Drive. Giới hạn 100 tệp, 100 MB/tệp, tổng vùng riêng 250 MB; truyền từng khối 512 KB. Chuẩn bị lỗi/hủy không thay lịch đã lưu. File không dùng được dọn sau lần lưu thành công (tránh xóa khi service đang phát). Xóa hết giờ rồi Lưu lịch sẽ dọn bộ âm thanh riêng. Không xóa dữ liệu bài học gốc. Khi đổi bài/thư mục hoặc thêm bài học xong, lưu lại để chuẩn bị đủ âm thanh mới; nếu bài chính mới chưa chuẩn bị, không phát nhầm bài cũ.

AlarmManager tính giờ địa phương, đặt lại mỗi lần qua ngày, khởi động lại máy, đổi giờ/múi giờ, cập nhật APK hoặc cấp lại quyền. Android 12+ cần quyền **Báo thức và lời nhắc** cho giờ chính xác; không có quyền dùng lịch không chính xác, có thể trễ và hệ thống có thể chặn khởi chạy media service từ nền. App hiển thị rõ trạng thái quyền và kết quả lần gần nhất.

Đến giờ: chỉ phát khi có tai nghe, không phát chồng bài đang nghe hoặc phiên hẹn khác. Không có tai nghe thì bỏ lượt, không chờ kết nối rồi phát bất ngờ. Lượt đến muộn quá 10 phút bị bỏ. Buộc dừng (Force Stop), hạn chế pin/OEM hoặc thiếu quyền có thể ngăn lịch chạy; không bảo đảm vượt qua các giới hạn này. Thời gian gần nhau chịu giới hạn exact alarm/Doze của Android.

Media3 service sở hữu player độc lập WebView, có audio focus/noisy/wakelock và điều khiển media chuẩn. Ngắt tai nghe dừng phiên. Báo cáo giây nghe và bài ôn hoàn tất được lưu private, nhập lại theo watermark để tránh cộng trùng. Bài ôn không đổi bài chính. Mỗi ngày một nhóm ôn, kết thúc mới tăng mốc ôn; native lưu cả hạn mới để không ôn lặp sớm khi chưa mở lại app. Không tạo tiến độ học xong cho bài mới chỉ do nghe theo giờ.

## Grok và DeepSeek: đánh giá, chưa thêm nhà cung cấp

Cả hai phù hợp để thử cho dịch, giải thích nghĩa/ngữ pháp, ví dụ theo ngữ cảnh và phản hồi văn bản. Không coi kết quả AI là thẩm định chính tả/ngữ pháp tuyệt đối. Nên đánh giá trên một tập câu Anh–Việt và so sánh chất lượng/độ trễ/chi phí trước khi đưa vào fallback mặc định.

- Grok/xAI có text, reasoning, structured output và công cụ tìm kiếm; các chức năng voice là API riêng cần kiểm chứng định dạng/timestamp và giá, không tự thay thế Whisper/căn phụ đề đang dùng.
- DeepSeek có API tương thích OpenAI, hữu ích cho xử lý văn bản/suy luận. Chọn model nhanh cho dịch/tra từ thay vì mặc định reasoning dài; khả năng và giá phụ thuộc model hiện hành.
- Tên tự đặt khi tạo key là nhãn quản lý, tùy ý. App phải chọn đúng provider/base URL, API format và model ID hợp lệ. Chỉ chuỗi key không đủ xác định tuyến gọi; không thử gửi key tới nhiều nhà cung cấp để dò. Gói chat của mỗi hãng không tự chứng minh đã có số dư API.
- Thiết kế nên có preset nhà cung cấp và model mặc định có thể chỉnh, tiếp tục cache/dedup/hàng đợi/retry hữu hạn và phân biệt 429/quota/số dư như AI Engine hiện có. Không phát sinh lời gọi trả phí trong đợt này.

Nguồn chính thức đã đối chiếu:
- https://docs.x.ai/overview
- https://api-docs.deepseek.com/en/ (kết quả tìm kiếm chính thức; trang trực tiếp timeout trong môi trường kiểm tra)
- https://developer.android.com/develop/background-work/services/alarms

## Kiểm thử

Kiểm thử Chrome mới: ẩn nút, ô tiếng Anh, nhật ký ở Phiên bản, mở rộng/thu gọn hàng đợi, hợp đồng native/chia khối, không lưu giả khi APK cũ, không đổi lịch khi thiếu âm thanh, chống nhập báo cáo trùng.

Java unit: giờ hôm nay/ngày mai, DST, giờ sai, ID đường dẫn và giới hạn lượt. Android instrumentation bổ sung: không có tai nghe thì bỏ lịch, không chạy hai lần cùng lượt, bỏ lịch đến quá muộn.

Cần kiểm thử điện thoại thật: tai nghe Bluetooth/USB, quyền exact alarm, khóa màn hình, hạn chế pin Samsung và Force Stop. Giả lập không chứng minh việc hẹn giờ thực tế với tai nghe Samsung.
