# Đánh giá nhập link video — 28/10/2026

Chỉ đánh giá, **chưa thêm chức năng hoặc gọi API video** trong bản beta53.

## Khả thi

Có thể làm trải nghiệm xem video và đọc bản dịch theo thời gian khi đã có file phụ đề có mốc thời gian (SRT/VTT), hoặc có file âm thanh/video mà người dùng được phép cung cấp để AI nhận dạng. App hiện đã có nhận dạng, dịch, căn thời gian, queue/cache và quản lý quota; phần cần bổ sung là trình xem video, đồng hồ video thay cho audio, nhập phụ đề và quản lý nguồn.

## YouTube

- IFrame Player API chính thức hỗ trợ nhúng video, play/pause/seek và đọc thời điểm phát; phù hợp để đồng bộ văn bản đi kèm. Đây không phải API tải âm thanh hay API lấy mọi transcript.
- `captions.download` yêu cầu tài khoản có quyền sửa video cùng OAuth; không thể coi API key Gemini/ChatGPT hay một link YouTube công khai là đủ để tải phụ đề. Phương thức có quota riêng, tài liệu hiện ghi 200 đơn vị/lượt.
- Nếu chỉ có link video của người khác: có thể nhúng nếu video cho phép nhúng, nhưng không bảo đảm app lấy được phụ đề gốc để tự dịch. Phụ đề có sẵn trong trình phát và văn bản app được cấp quyền truy cập là hai việc khác nhau.
- Không khuyến nghị thiết kế phụ thuộc vào trích xuất âm thanh/transcript bằng endpoint không chính thức: dễ hỏng khi trang thay đổi, có thể cần đăng nhập, và các hạn chế của nền tảng cần được tuân thủ. Tài liệu YouTube cũng giới hạn tải/lưu nội dung nghe nhìn và phát nền trong API client.

Nguồn đã đối chiếu:
- https://developers.google.com/youtube/iframe_api_reference
- https://developers.google.com/youtube/v3/docs/captions/download
- https://developers.google.com/youtube/terms/developer-policies
- https://developers.google.com/youtube/terms/developer-policies-guide

## Facebook

Cần thử riêng video công khai, video của chính người dùng/Page, Reels và video cần đăng nhập. Nhúng để xem không mặc nhiên cho app quyền truy cập file âm thanh hay phụ đề. Chưa xác minh được đầy đủ API/quyền hiện tại: hai trang tài liệu Meta chính thức bên dưới không tải được trong môi trường nghiên cứu. Vì vậy chưa cam kết hỗ trợ link Facebook bất kỳ, chưa triển khai trình trích xuất hay thu thập cookie/tài khoản.

Trang cần đối chiếu tiếp trước khi thiết kế:
- https://developers.facebook.com/docs/plugins/embedded-video-player/
- https://developers.facebook.com/docs/graph-api/reference/video/captions/

## Đề xuất

1. Giai đoạn đầu: video/âm thanh do người dùng nhập, hoặc video nhúng kèm SRT/VTT/văn bản được cung cấp hợp lệ. Hiển thị song ngữ theo câu và tra từ như app hiện tại.
2. Sau đó: YouTube có quyền quản lý video qua OAuth; tự lấy phụ đề được cấp quyền, dịch cache theo nội dung, điều khiển video qua API chính thức.
3. Facebook chỉ đưa vào sau một bản thử nghiệm xác minh được nguồn và quyền; thông báo rõ khi không lấy được phụ đề, không hứa mọi link đều dùng được.

ASR và dịch vẫn phụ thuộc chất lượng âm thanh, ngôn ngữ, mạng, quota/chi phí và thời gian xử lý. Không chuyển qua một nhà cung cấp khác hoặc thử lại vô hạn khi hết quota. Chưa thực hiện thử nghiệm OAuth/video thực tế trong lượt này.
