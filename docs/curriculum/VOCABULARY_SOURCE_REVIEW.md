# Chọn nguồn từ chuẩn — đề xuất để duyệt

Đối chiếu ngày 28/10/2026. Chưa nhập hoặc đánh dấu duyệt một danh sách 3.000 từ. Danh mục hiện có 173 mục đề xuất qua các bản nháp, gồm 171 từ đơn và 2 cụm; đây vẫn là bảng biên soạn, không phải nguồn chuẩn đã được duyệt. Xem `VOCABULARY_AUDIT.md` để kiểm tra số lượng và phạm vi.

## Nguồn đã đọc trực tiếp

- [Trang chính thức NGSL 1.2](https://www.newgeneralservicelist.com/new-general-service-list) công bố 2.809 mục và giấy phép CC BY-SA 4.0 của Browne, Culligan và Phillips; có bản thứ hạng, bản lemmatized và 52 từ bổ sung không nằm trong các file xếp hạng đầu. Vì vậy không gắn nhãn “NGSL = đúng 3.000 từ”. Cần kiểm tra file cụ thể sau chuẩn hóa/gộp trùng.
- [Thông báo của NGSL Project](https://www.newgeneralservicelist.com/) xác nhận dùng miền `.com` làm nguồn chính thức, không dùng miền `.org` cũ.
- [NOTICE của wordfreq](https://github.com/rspeer/wordfreq/blob/master/NOTICE.md) phân biệt mã Apache 2.0 và dữ liệu CC BY-SA 4.0, có yêu cầu ghi công. Dữ liệu tần suất hiện có trong app không tự trở thành 3.000 lemma đã duyệt; cần giữ nguồn/ghi công nếu dùng làm dữ liệu dẫn xuất.

Đây là kết quả kiểm tra tài liệu nguồn, chưa phải quyết định xuất bản danh sách hoặc xác nhận tuân thủ mọi nghĩa vụ của một sản phẩm thương mại.

## Phương án đề xuất

1. Lấy NGSL 1.2 làm ứng viên nền vì có các bản phục vụ dạy học/lemma và nguồn rõ; lưu nguyên file nguồn, URL, phiên bản, SHA-256, tác giả và giấy phép khi nhập.
2. Nhập bản bổ sung bằng quy tắc riêng, không đếm lặp từ đã có. Sau khi chuẩn hóa, tính đúng số mục còn thiếu đến 3.000; chưa tự lấp bằng tên riêng, số đếm hoặc dạng biến đổi.
3. Chọn phần bổ sung theo nhu cầu giao tiếp của 12 chủ đề đã thống nhất, giải trình từng mục, không ép thêm từ nghề nghiệp quá chuyên sâu chỉ vì David là kiến trúc sư.
4. Người dùng duyệt nguồn, cách đếm và phần bổ sung trước khi chuyển `pending` sang danh sách chuẩn. Để bảng từ/dữ liệu dẫn xuất kèm thông tin giấy phép riêng; chưa suy diễn toàn bộ mã app hoặc truyện phải đổi giấy phép theo danh sách.

## Quy tắc đếm đề xuất

- Chữ thường, chuẩn hóa Unicode, giữ một ID ổn định cho mỗi đầu mục được duyệt. Biến thể rõ như child/children, wife/wives, go/went không làm tăng số mục.
- Từ có nhiều nghĩa hoặc từ loại cần ghi sense/POS, nhưng không tự nhân số “từ mới” chỉ bằng cách tách nhiều nghĩa của cùng một đầu mục.
- Dạng dễ mơ hồ như leaves (leaf/leave), saw, drawing cần quyết định theo ngữ cảnh hoặc gắn nhãn thủ công. Báo cáo hiện tại vẫn có giới hạn này.
- Cụm như make a difference là mục luyện theo ngữ cảnh. Mặc định không tính thêm vào tổng 3.000 từ đơn; lưu một danh mục chunks riêng. Cần chốt nếu muốn thay mục tiêu thành 3.000 entries gồm cả cụm.
- Tần suất xuất hiện khác bằng chứng người học nhận biết/hiểu/sử dụng được. Giữ ba kỹ năng và lịch ôn độc lập với số lượt xuất hiện.

## Điều kiện trước khi nhập danh sách chuẩn

Có file cụ thể và giấy phép đi kèm; parser kiểm tra trùng/thiếu ID/dạng từ; quy tắc xử lý đầu mục/cụm được duyệt; có báo cáo trước và sau chuẩn hóa; không báo đạt 3.000 khi chỉ có mục tiêu trong manifest. Chưa gọi dịch vụ trả phí để làm bước này.

## Quyết định cho bản beta theo yêu cầu người dùng

Người dùng yêu cầu hoàn thiện danh sách, phân bổ và cập nhật bản xem trong app. Do tải nguồn NGSL 1.2 trực tiếp bị từ chối HTTP 403 và bản mirror tìm được không còn truy cập được, chọn **NGSL 1.01** 2.801 đầu mục từ mirror ghim commit, bổ sung 199 đầu mục theo ba nhóm chủ đề sinh hoạt/gia đình, thực phẩm/môi trường và công việc/di chuyển. Không giả danh bản 1.2, không gán thứ hạng tần suất cho phần bổ sung. Tên riêng, số đếm, dạng biến đổi và biến thể chính tả không dùng để lấp đủ số.

Chi tiết nguồn và phép biến đổi: `curriculum/sources/ngsl-1.01/NOTICE.md`. Danh sách chuẩn dùng cho biên soạn beta: `curriculum/vocabulary.standard.json`; phân bổ: `curriculum/vocabulary.allocations.json`. Các tệp này có vòng đời riêng với bản manifest bàn giao v1 (vẫn lưu làm mốc ban đầu). Xem `STANDARD_VOCABULARY_PLAN.md`. Trạng thái chốt danh sách beta không đồng nghĩa mọi từ đã có trong truyện hoặc nội dung đã được duyệt xuất bản.
