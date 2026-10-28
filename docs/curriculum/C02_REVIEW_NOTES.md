# C02 — The House That Listened: bản nháp để duyệt

## Câu chuyện

Một vài ngày sau C01, David thử hai bản thiết kế bằng vạch băng dính trên sàn nhà mình trước khi gặp chủ nhà tương lai. Anna đang kiểm tra số liệu kế toán; các con cùng dùng không gian khiến công việc bị ngắt quãng. Bà Helen đến thăm và thử ngồi, giúp cả nhà phát hiện lối đi trên hình không đủ chỗ khi có người thật.

David ban đầu thêm phòng cho từng yêu cầu. Anna đặt lại câu hỏi về ngân sách và điều quan trọng nhất, rồi cả nhà thử một kế hoạch hai mươi phút yên tĩnh. David học cách nghe nhu cầu khác nhau, đo thay vì đoán, và hỏi người sẽ ở trước khi quyết định. Lily vẽ chiếc tai cạnh quả cà chua từ C01. Cuối bài, hai bản vẫn nằm trong bìa hồ sơ xanh, sẵn sàng nối sang C03 — The Missing Drawing.

Không có nhà mới được xây trong C02. Vạch xanh không ngăn tiếng ồn; cánh cửa trong **nhà thật đang ở** và cách phân chia hoạt động mới giúp Anna tập trung. Gia đình David chỉ thử ý tưởng, không thay thế nhu cầu của khách hàng. Chưa có dự toán xây dựng cụ thể, không bảo đảm một phương án sẽ rẻ hoặc vừa ngân sách.

## Nhân vật và tính liên tục

- Gia đình vẫn đúng năm người: David, Anna, Ben lớp 4, Lily lớp 1, Sophie hai tuổi. Không tăng tuổi/lớp chỉ vì sang bài mới.
- Anna là nhân vật chủ động: cần không gian làm việc, phân tích ngân sách và đưa ra câu hỏi. Không giới hạn cô ở việc nội trợ hoặc chỉ nhắc tiền.
- Grandma Helen là mẹ David, bà nội ba bé, đến thăm; không trở thành thành viên thứ sáu của gia đình chính. Không tự chốt bà sống chung hoặc sống ở đâu.
- Bài chỉ đưa thêm bà Helen vào cảnh trực tiếp; ông Robert, cậu James, Tom và Michael chưa bị đưa vào cho đủ danh sách tên. Dàn con giáp giữ độc lập.

## Bốn phần đã viết

| Phần | Nội dung |
|---|---|
| Main Story | Tám cảnh, tiếng Anh và bản dịch Việt |
| Word Discovery | Chín mục được chọn: budget, afford, quiet, noise, privacy, measure, choice, agree, plan |
| Quick Response | 64 câu theo tám cảnh, chờ sáu giây trước đáp án mẫu; không chấm khớp nguyên văn |
| Time Shift | Hôm qua, góc nhìn Anna, cuộc gặp tương lai giả định, góc nhìn bà Helen; có bản dịch Việt |

Câu hỏi đều trỏ đến cảnh trong chính C02. Các chi tiết cần để trả lời được nhắc trong bài, không buộc học thuộc C01 hoặc học truyện mở rộng.

Ước tính 2.673 từ được đọc và 384 giây chờ: khoảng **30,7 phút ở 110 từ/phút**; dải giả định 100–120 từ/phút khoảng 28,7–33,1 phút. Đây là tính từ kịch bản, không phải thời lượng audio thật. Cần nghe thử/đo trước khi chốt 30–45 phút.

## Từ vựng

Thêm 12 đầu mục đề xuất về không gian và quyết định: budget, afford, quiet, noise, privacy, measure, choice, agree, plan, cost, space, share. Bảng biên soạn chung hiện có 60 mục, không phải 60 từ mới đã được học hoặc danh sách chuẩn 3.000 từ.

Giữ biến thể như agree/agreed nhưng chưa tự gộp danh từ phái sinh agreement vào agree, hoặc measurement vào measure. Cụm từ vẫn cần chính sách riêng. IPA, trình độ dự kiến A2 và lựa chọn Word Discovery chờ rà soát; chưa có kết quả đo người học hay bằng chứng nhớ từ.

## Công cụ và bản đọc

- Bản đọc riêng: `C02_DRAFT_V1.md`; dữ liệu cùng `curriculum/drafts/v1/scripts.json` để dùng ID và QA chung.
- Review và preview nhận mọi bài đã có **đủ phần**, không còn khóa cứng sáu phần C01/Z01; bài thiếu phần hoặc nguồn câu hỏi sai bị chặn. Placeholder ban đầu vẫn giữ riêng, không biến thành nội dung đã biên soạn.
- Word Discovery kiểm tra theo từng bài, có thể chọn lại từ cần ôn ở bài sau; không buộc từ đã gặp chỉ được giải thích một lần.
- Có thể xuất riêng bài bằng `node tools/curriculum/review-draft.cjs --markdown --lessons C02`. Bản HTML hiển thị C01, C02, Z01 và hủy timer đúng khi đổi bài.
- Z01 vẫn liên kết C01 theo manifest; nhóm mở rộng gắn với C02 là Z06–Z10, chưa tự đổi Z02 thành bài ôn C02.

Kiểm thử cục bộ: **69/69 Node đạt**, check-web đạt; Chrome 360px đã thử C02 đủ bốn phần, bản dịch, câu hỏi/nguồn đúng và đổi về C01 không giữ timer cũ. Không sửa app/native/signing hoặc version phát hành. Chưa thử audio thật, không gọi API trả phí, chưa merge/phát hành.

## Cần duyệt tiếp

Nhịp truyện có dễ nghe không, Anna/bà Helen có tự nhiên không, 64 câu có quá lặp không, cần giản lược từ/cấu trúc nào cho người mới. Sau khi có phản hồi mới chốt bản nội dung và mẫu giọng. Không tự coi chỉ số QA là quyết định duyệt.
