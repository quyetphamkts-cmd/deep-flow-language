# C03 — The Missing Drawing: ghi chú duyệt

## Cốt truyện và tính liên tục

David chuẩn bị gặp Michael ở thư viện lúc ba giờ để xem hai bản vẽ trước cuộc trao đổi với chủ nhà. Bản thứ hai có cà chua/cái tai/ghi chú từ C01–C02 không còn trong bìa hồ sơ xanh. Thấy giấy thừa cạnh cầu của Ben, anh vội nghi con đã lấy. Anna hỏi rõ tờ nào và phân biệt điều biết với điều đoán; Michael qua điện thoại hỏi lần cuối David dùng ở đâu. Ben kể lại tối hôm trước: chính David mang bản vẽ đến bàn cửa sổ, chặn nó bằng sách xanh rồi đi xem cầu của con.

Giấy nằm dưới sách, góc gập nhưng không rách. David tìm ra, nhận lỗi và nghe nỗi lo của Ben trước khi đi. Anh có bản sao, cất bản gốc rõ chỗ, vẫn đến hẹn sớm vài phút. Michael đề nghị cùng nghe người ở làng gần đó trao đổi về không gian chung — nối sang C04, chưa phải chuyến đi đã diễn ra.

- Tối trong C02 kết thúc bằng cất cả hai bản vào bìa. C03 nói rõ David lấy bản thứ hai **ra sau đó**, nên không tự mâu thuẫn vị trí.
- Giấy xanh của Ben được cho phép dùng làm cầu; kéo của Anna được mượn rồi trả. Không dùng borrow cho giấy tiêu hao rồi mặc định phải trả lại nguyên tờ.
- Ben không là người lấy bản vẽ, không phải chịu trách nhiệm chỉ vì đứng gần bàn. David xin lỗi về hành động cụ thể thay vì chỉ mong con hết buồn.
- Michael vẫn là bạn lâu năm, chưa tự gán nghề kiến trúc sư hoặc người thẩm định chuyên môn. Buổi xem cùng bạn chưa thay cuộc gặp chủ nhà/người xây.
- Câu chuyện không hứa chụp ảnh hay sắp xếp giấy sẽ loại bỏ mọi sai sót; thói quen bảo quản và giao tiếp đều cần thiết.

## Bốn phần

| Phần | Bản nháp đã có |
|---|---|
| Main Story | Tám cảnh; tiếng Anh và bản dịch Việt |
| Word Discovery | Missing, borrow, permission, guess, evidence, blame, relief, apologize, copy; có lý do chọn |
| Quick Response | 64 câu, mỗi câu trỏ đúng cảnh C03, chờ ba giây rồi có đáp án mẫu |
| Time Shift | Nhìn lại hôm qua/tối trước; góc nhìn Ben; thói quen tương lai và chuyến làng giả định; có dịch Việt |

Các chi tiết cần trả lời đều nằm trong C03, không yêu cầu học thuộc bài cũ. Đáp án là mẫu, không yêu cầu người học nói khớp nguyên văn. Q&A dùng tiếng Anh, mới dịch phần hướng dẫn đầu, không giả vờ đã có đủ phụ đề song ngữ theo thời gian.

Sau thay đổi pause theo người dùng: 2.811 từ được đọc và 192 giây chờ, khoảng **28,8 phút ở 110 từ/phút**; dải giả định 100–120 từ/phút xấp xỉ 26,7–31,3 phút. Chưa có giọng đọc thật hoặc đo audio, chưa chứng nhận đạt thời lượng. Word Discovery chỉ chọn chín mục theo giả thuyết biên tập, không giải thích mọi từ chưa biết.

## Phạm vi dữ liệu

Thêm 12 mục đề xuất cho bài: missing, borrow, permission, guess, evidence, blame, relief, apologize, copy, appointment, remember, return. Bảng chung hiện có **72 đầu mục đề xuất**, không phải 72 mục mới đã học, không phải 3.000 từ chuẩn và chưa chứng minh có đủ 250 từ mới/bài. Danh từ phái sinh như apology không tự được gộp thành một dạng chia của apologize.

Các script C01/C02/Z01 được giữ nguyên; bản Markdown được sinh lại do phiên bản chung và bảng từ đã thay đổi. Tên/tuổi/lớp và quan hệ gia đình không đổi. Michael là nhân vật phụ mới duy nhất có vai trò trực tiếp lần đầu; không dồn mọi họ hàng vào bài.

## Đọc và kiểm tra

```sh
node tools/curriculum/review-draft.cjs --markdown --lessons C03
node tools/curriculum/build-preview.cjs > /tmp/deepflow-curriculum-preview.html
node --test tests/*.test.cjs
node tests/curriculum-preview.browser.cjs
```

Bản đọc riêng là `C03_DRAFT_V1.md`; HTML có C01/C02/C03/Z01. Công cụ kiểm tra đủ phần, câu hỏi/đáp án/pause, nguồn trong cùng bài và các mục Word Discovery. Thử Chrome màn hình 360px kiểm tra chọn C03, đủ phần, nguồn câu hỏi và Time Shift; không tự chấm nghĩa hay hiệu quả học.

Kiểm thử cục bộ: **71/71 Node và Chrome đạt**, check-web đạt. Chưa kiểm thử audio thật hoặc app tích hợp vì chưa sản xuất/tích hợp phần đó. Chưa sửa UI/native/signing/version phát hành, chưa gọi API trả phí, chưa merge/phát hành.

## Điểm cần đọc thử

Nhịp truyện có đủ căng thẳng mà không nặng lời không; Ben có cảm xúc tự nhiên không; nhóm từ evidence/blame/relief có cần câu giải thích ngắn hơn không; 64 câu có quá lặp không; ba giây có tạo nhịp phản xạ phù hợp không. Chỉ số QA không thay quyết định duyệt nội dung của người dùng.


**Quy tắc phản xạ đã chốt:** cả 237 câu Quick Response trong bộ hiện tại đều chờ ba giây. Một/vài từ là đủ; câu mẫu dài hơn chỉ để nghe tham khảo, không phải điều kiện đạt. Mục tiêu 30–45 phút vẫn là hướng biên soạn, nhưng chưa được chứng nhận ở tốc độ/giọng đọc thực. Không kéo dài chờ để bù số phút; cần nghe/đo và hoàn thiện nội dung có ích sau khi duyệt.
