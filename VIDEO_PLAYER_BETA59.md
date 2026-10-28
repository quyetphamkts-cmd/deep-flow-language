# Video và thay đổi Đọc — beta.59

## Giao diện tách riêng

- Nút **Video ▶** cạnh **Đọc** mở khu vực video riêng. Không thay thư viện khóa học, bài chính, lịch ôn hoặc dữ liệu từ vựng.
- Cần APK lõi **28 / 1.0.2-beta.60** để dùng Media3; APK cũ vẫn xem hướng dẫn cập nhật, các chức năng web khác vẫn dùng được. Cài đè bằng khóa ký cũ, không cần xóa app/dữ liệu.
- Link trực tiếp HTTPS `.mp4`, `.m4v`, `.webm`, `.m3u8` hoặc file được chọn bằng Android Storage Access Framework. Không nhúng/trích xuất YouTube/Vimeo ở bản này. Video mã hóa/DRM, đường trang HTML, link hết hạn hoặc codec không tương thích có thể không phát được; app báo lỗi và không thử lại vô hạn.
- Media3 ExoPlayer/MediaSessionService sở hữu một player độc lập WebView. Vào nền/khóa màn hình tắt track hình, tiếp tục audio; quay lại dùng cùng player/vị trí. HLS có thể chọn audio riêng; MP4 ghép hình/tiếng vẫn có thể tải dữ liệu hình. Không hứa phát tiếp sau Force stop/Android kết thúc cả tiến trình.
- Audio focus và trở thành noisy (tháo tai nghe) do player xử lý. **Dừng / Đóng** kết thúc, **Nghe nền** đóng giao diện và giữ phát. Bắt đầu bài nghe hoặc mở Đọc dừng player video và lưu vị trí. Không tạo hai thông báo phát cùng lúc.
- Lịch sử video tối đa 20 nguồn, vị trí ghi định kỳ khi đang phát và lúc dừng; chỉ lưu trong vùng riêng thiết bị. URL có query/token không đưa vào log, backup cloud/device-transfer của prefs video bị loại trừ. Quyền đọc file có thể phải chọn lại nếu provider không cấp quyền lâu dài.

## Chủ đề và đề xuất

- Chủ đề tìm kiếm/video đã mở dùng để tìm liên quan trong video đã thêm và Wikimedia Commons; hiển thị tác giả, giấy phép và đường nguồn. Không phải AI suy đoán sở thích/trình độ.
- API công khai `query`, `generator=search`, `filetype:video`, `imageinfo` lấy URL/mime/extmetadata. Chỉ nhận MP4/WebM ở upload.wikimedia.org có nhãn CC BY/CC BY-SA/CC0/Public domain. Kết quả giữ credit ở lịch sử và màn hình phát; chạm credit để xem nguồn. Không tải hoặc phát tự động clip mới.
- Cache tối đa 20 truy vấn trong phiên, hạn 6 giờ; hủy truy vấn cũ/bỏ phản hồi muộn. Nhập chủ đề rồi bấm tìm hoặc dùng lại chủ đề của video đã mở; không gọi AI trả phí. Có thể không có kết quả và video có thể không có lời thoại/ngôn ngữ người học muốn; cần xem/nghe kiểm tra.
- Tài liệu: [MediaWiki Imageinfo](https://www.mediawiki.org/wiki/API:Imageinfo), [Commons Search](https://commons.wikimedia.org/wiki/Help:Search), [Reuse](https://commons.wikimedia.org/wiki/Commons:First_steps/Reuse).

## Phụ đề và thống kê

- Nhập SRT/VTT nhỏ hơn 2 MB hoặc chọn dùng phụ đề bài nghe hiện tại khi video có cùng nội dung/timeline. Phụ đề bài nghe là bản sao, không sửa bài nghe. File song ngữ hiển thị các dòng do người dùng cung cấp. Caption track có trong luồng cũng có thể chọn bằng CC nếu nguồn/codec hỗ trợ.
- Nút **Từ vựng** trong video tính từ cue của file phụ đề đã nhập: tổng token, số dạng từ khác nhau và tần suất (liệt kê tối đa 200 dạng phổ biến). Bỏ timestamp, cue ID, tag, NOTE/STYLE/REGION; không coi chúng là lời thoại.
- Không có file text: báo cần SRT/VTT trước khi thống kê toàn bài. Không suy ra vốn từ, số họ từ hoặc từ đã thuộc. Phụ đề song ngữ đếm cả hai ngôn ngữ; không gắn nhãn sai là vốn từ tiếng Anh.
- **Chưa có ASR/dịch trực tiếp video/HLS hoặc tải transcript Vimeo tự động.** Có thể dùng phụ đề đã tạo bằng công cụ hiện có khi cùng nguồn/timeline, hoặc nhập file nguồn; không gọi API trả phí trong phát triển/kiểm thử. Nội dung từ phụ đề nguồn và kết quả AI khác nhau, không bảo đảm đúng tuyệt đối.

## Trong nút Đọc (không liên quan lịch sử video)

- Ảnh OCR có **Hủy / Ẩn**: Ẩn để thao tác mục khác, công việc độc lập với trang đang xem; hoàn tất lưu Lịch sử đọc và báo mở kết quả, không tự chuyển khỏi công việc hiện tại. Đóng Đọc cũng không hủy OCR.
- Hủy đánh dấu lượt đã hủy, ngắt fetch/chờ AI khi được hỗ trợ và worker OCR riêng; bỏ phản hồi muộn, không chuyển nhà cung cấp hay gửi ảnh tiếp. Yêu cầu nhà cung cấp đã nhận có thể vẫn được tính phí; không hứa hủy là hoàn phí.
- Tác vụ chưa xong nằm trong tiến trình hiện tại: Android đóng tiến trình/Force stop có thể mất công việc dở, chưa có hàng đợi OCR bền vững qua việc app bị đóng. Kết quả đã lưu có thể mở ở lịch sử sau.
- Lịch sử đọc nhấn giữ chọn, chọn tất cả trong mục, kéo ☰ sắp xếp, sắp theo tên/ngày, chuyển vào **Lưu trữ** hoặc thư mục con tạo bằng tên, xóa có xác nhận và hoàn tác lượt sửa gần nhất. Có bộ lọc toàn bộ/chưa lưu trữ/từng thư mục.
- Xóa tài liệu không xóa thống kê đã xác nhận đọc xong hoặc từ vựng đã lưu. Không âm thầm cắt còn 25 bài; mở lại bài giữ vị trí trong danh sách/thư mục. Hết localStorage báo rõ, giữ bản cũ nguyên vẹn, không loại bớt tài liệu để cố lưu.

## Kiểm thử

- Node: xác thực URL video + kiểm thử các chức năng hiện có.
- Chrome: giao diện video tách riêng/cổng APK cũ; source/license/XSS/cache của gợi ý; bàn giao source có thứ tự dừng session cũ; OCR Ẩn/Hủy/phản hồi muộn; hủy fetch/hàng đợi; history chọn/kéo/chuyển/xóa/hoàn tác/quota và giữ thống kê.
- Java: chính sách nguồn/lỗi; phân tích SRT/VTT, loại metadata, đếm song ngữ mà không giả là họ từ.
- Android 16 emulator: fixture MP4/âm thanh tự tạo, sidecar SRT, player thật trong service; đưa UI ra nền vẫn chạy, tắt track hình/giữ vị trí, quay lại, tắt/bật màn hình, dừng/lưu vị trí. Tệp fixture base64 là dữ liệu kiểm thử, không phải nội dung khóa học.
- Kiểm thử gợi ý dùng dữ liệu mô phỏng, chưa xác nhận API Commons trên mạng/thiết bị thực của người dùng. Chưa thử Samsung thật, tai nghe thật, video dài, DRM hay mọi codec; không gọi AI trả phí. CI/build cần đạt trên đúng commit cuối trước khi phát hành.
