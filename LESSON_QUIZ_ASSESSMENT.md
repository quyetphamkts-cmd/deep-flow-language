# Nghiên cứu trắc nghiệm sau bài học

## Kết quả khả thi

App đã có văn bản/phụ đề theo bài (`lessonText`, `buildCuesFrom`) và mức nhớ khi đánh dấu học xong (`pickMem`). Vì vậy có thể tạo bộ câu hỏi chỉ từ nội dung bài, chấm điểm và dùng làm một loại bằng chứng cho mô hình ôn. Bảng tự đánh giá mở ngay khi bấm Đánh dấu học xong; hủy đánh giá không tự hoàn tất thao tác. Beta.61 có bản trắc nghiệm AI thử nghiệm trong bảng đánh giá từng bài; chỉ gọi AI khi người học chủ động xác nhận trong app. Không gọi AI trả phí trong quá trình phát triển/CI.

Đã có 5 câu mẫu trong `docs/learning/quiz-example-c01.json` lấy từ bản nháp C01-main_story, mỗi câu có 4 lựa chọn, chỉ mục đáp án, giải thích, ID đoạn nguồn và hash của văn bản nguồn. Đã kiểm tra cấu trúc/ID tham chiếu. Đây là mẫu nghiên cứu, không phải ngân hàng đã duyệt để phát hành. Không sửa bài giáo trình hoặc sử dụng nội dung người học từ điện thoại.

## Cơ chế đã triển khai

Sau khi bấm học xong, bảng mở ngay với hai mục tách biệt: Người học tự đánh giá hoặc Đánh giá bằng trắc nghiệm. Nếu chưa có đủ văn bản đáng tin cậy, giữ tự đánh giá; không bịa câu hỏi từ tên bài. Ngân hàng 24 câu theo hash văn bản, mỗi lượt chọn 8 câu chưa dùng; hết ngân hàng thì chọn lại nhưng tránh bộ vừa làm. Tráo Fisher–Yates cả thứ tự câu và lựa chọn, cập nhật chỉ mục đáp án đúng. Bộ ngắn 8 câu: chi tiết, thứ tự/nguyên nhân, hiểu ý và từ/cụm trong đúng ngữ cảnh. Mỗi câu cần đoạn dẫn chứng; không hỏi kiến thức ngoài bài. Không chỉ lấy từ bất kỳ làm câu điền khuyết rồi gọi đó là đo mức hiểu.

Câu hỏi AI là bản thử nghiệm, không có thẩm định ngữ nghĩa độc lập: bắt buộc đúng schema, một đáp án rõ ràng, lựa chọn không trùng/ngầm chứa đáp án, dẫn chứng khớp hash và segment ID hiện tại, đáp án được kiểm chứng lại với nguồn. Tác vụ thẩm định bằng AI vẫn có thể sai; nên có ngân hàng câu đã rà soát cho giáo trình chuẩn và nút báo câu mơ hồ cho bài nhập. Chưa có câu đã thẩm định thì không dùng điểm làm chứng nhận trình độ.

Tạo một lần theo bài + hash văn bản + ngôn ngữ + độ khó, lưu local/cache; sửa phụ đề làm vô hiệu bộ cũ. Không gọi lại mỗi lần mở, không tự gửi tài liệu lên API trả phí. Nếu dùng AI online, cần thao tác chủ động được cho phép, thông báo nội dung gửi và giữ hàng đợi/retry hữu hạn của AI Engine. Phương án không trả phí trước mắt là ngân hàng biên soạn/rà soát sẵn hoặc mô hình local có kiểm soát chất lượng; hiệu quả local chưa được kiểm chứng.

## Điểm và lịch ôn

Điểm đúng/tổng câu, phân riêng mức hiểu và nhớ chi tiết. Bộ 5 câu có bước điểm 20%, không được giả vờ chính xác 1%. Chỉ lần làm đầu độc lập được dùng như tín hiệu mạnh; làm lại bộ vừa thấy đáp án hoặc chọn đoán không chứng minh đã nhớ lâu. Lưu lượt làm, phiên bản đề, thời điểm, đáp án, thời gian phản hồi và mức tự tin nếu có. Phản hồi giải thích sau khi nộp, không lộ đáp án trước.

Gắn evidence=quiz-sample vào mô hình khi dùng điểm bộ có ít nhất 5 câu hợp lệ; score thấp rút ngắn khoảng ôn, cao chỉ giãn có giới hạn. Không tự thay điểm tự báo cũ bằng điểm từ bộ chưa duyệt. Hủy/thoát đề không tính sai toàn bài hoặc xóa thời gian đã học. Native nghe nền chưa có bài kiểm tra tương tác thì tiếp tục chỉ ghi exposure.

## Kiểm thử trước khi bật

- Câu chỉ tham chiếu đúng bài/hash/đoạn, không dùng kết quả của bài khác hoặc văn bản đã sửa.
- Một đáp án, lựa chọn khác nhau, tráo vị trí mà không đổi chấm điểm, không lộ đáp án qua HTML/nhãn UI.
- Nộp hai lần hoặc quay lại màn hình không cộng lượt/điểm hai lần; thoát đề không phá bài chính/dữ liệu cũ.
- Điểm đúng và độ tin cậy được hiệu chỉnh bằng người đọc thực tế; bộ câu mơ hồ không làm người học bị lên lịch sai.
- Kiểm thử offline, app đóng, API thiếu key/429/quota và cache; không gọi API trả phí trong CI.

Sau Nộp bài, hiển thị số đúng/tổng và %. Người dùng phải bấm Sử dụng điểm mới ghi vào mức nhớ/lịch. Đề AI có thể sai; câu mơ hồ có thể bỏ qua, còn dưới 5 câu hợp lệ không chấm. Nếu câu trong bộ đã làm trong 24 giờ thì chỉ ghi luyện tập (evidence=quiz-practice), không tăng mức nhớ theo điểm làm lại. Đổi văn bản vô hiệu đề cũ và chặn áp điểm cũ. Hủy không đổi trạng thái bài hay điểm; hủy sau khi gửi AI có thể vẫn tốn lượt. Bộ đề cache tối đa 20 bài, có kiểm soát schema/dẫn chứng nhưng chưa kiểm định độ chính xác sư phạm. Điểm đúng/tổng là điểm trên mẫu câu hỏi, không đồng nhất với % nhớ toàn bộ bài.

Kết quả tạo đề đi qua aiJSON với cache:false: ngân hàng đã qua kiểm tra được cache riêng; phản hồi sai schema không bị AI Engine cache 30 ngày, nên lần bấm tạo lại có thể thử mới sau khi người dùng xác nhận. Không tự lặp tạo đề khi thất bại.
