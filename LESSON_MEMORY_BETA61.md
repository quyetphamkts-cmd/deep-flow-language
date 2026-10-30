# Lịch ôn từng bài — beta.61

Mô hình quên lãng thực nghiệm đơn giản, không phải FSRS/SM-2 và không đảm bảo biết chính xác lúc người dùng quên. Bản thân mô hình nhớ tính trên máy, không gọi AI/API; tính năng tạo đề trắc nghiệm riêng chỉ gọi AI khi người dùng đồng ý.

## Dữ liệu và công thức

Mỗi bài audio đã học xong có `st.lessonReview.memory`: version, last, interval90, difficulty, reviews, ratings, evidence và due. Không gom các bài cùng thư mục vào một trạng thái nhớ. Bỏ đánh dấu học xong vẫn xóa riêng lịch của bài như trước; không xóa thời gian đã nghe.

`R(t) = 0.9 ^ (số ngày từ lần học/ôn cuối / interval90)`.

`interval90` là số ngày mô hình dự đoán còn nhớ 90%. Đặt hạn ở mức 92%: `last + interval90 × ln(0.92)/ln(0.9) ngày` (~79% interval90). Đây là biên an toàn dự đoán, không bảo đảm không quên. Mức nhớ tự báo ban đầu tạo prior `0.5 + 2.5 × (score/100)^2` ngày; thiếu số liệu dùng 75%. Lần đầu giữ mốc không muộn hơn 1 ngày. Khoảng cách được chặn 0,125–365 ngày để không về 0 hoặc tăng vô hạn.

Sau lượt ôn:
- Có tự đánh giá: điểm thấp (<60%) rút ngắn khoảng; điểm tốt tăng khoảng dựa trên điểm, độ khó và mức nhớ dự đoán lúc ôn. Độ khó điều chỉnh 15% theo lượt mới.
- Không đánh giá/chỉ nghe: chỉ tăng nhẹ interval90 ×1,2, evidence=exposure; không tăng số lượt đã xác nhận nhớ. Không tự đánh dấu bài mới đã học xong.
- Kết thúc audio khi app mở: hỏi mức nhớ; Hủy vẫn ghi nhận đã nghe. Khi app ẩn/native hẹn giờ: không ép hiện hộp thoại, chỉ ghi exposure.

Trạng thái đến hạn được sắp theo `due`, rồi `completedAt` và ID. Không còn khóa một mục/ngày hoặc tự dời các bài chờ sang ngày hôm sau: những ràng buộc đó có thể khiến bài đã đến hạn phải chờ quá lâu. Thu gọn vẫn chỉ hiện bài đầu, mở rộng hiển thị toàn bộ. Mỗi bài đến hạn có thể mở ôn, bài chính được khôi phục khi dừng/ôn xong. Cập nhật hàng đợi khi quay lại app và mỗi phút ở Thư viện.

## Dữ liệu cũ / native

Lịch cũ được chiếu sang mô hình mà không ghi/xóa dữ liệu khi đọc; giữ `due` cũ nếu sớm hơn hạn dự đoán. Mô hình riêng chỉ được lưu sau ôn. Không đẩy một hạn cũ sang muộn chỉ vì nâng cấp.

Java `StudyMemory` dùng cùng R(t), due và công thức cập nhật với web. Service chọn từng bài đến hạn sớm nhất, không khóa bởi marker một mục/ngày của beta.60. Mô hình native lưu theo ID, gửi cùng báo cáo hoàn tất để web nhập đúng một lần. Chỉ dùng bản native mới nếu `last` không cũ hơn snapshot và ngày hoàn thành gốc; giữ mô hình web mới hơn. Dịch vụ hẹn giờ vẫn cần APK mới, quyền báo thức, tai nghe và âm thanh local đã chuẩn bị; Force Stop/hạn chế pin có thể ngăn tự phát.

Đồ thị trong Thư viện hiển thị đường dự đoán, mốc 92% và hiện tại; trục đứng 80–100%, giá trị thấp hơn bị cắt trên hình nhưng con số được hiển thị đầy đủ. Hiển thị số lượt có/không tự đánh giá. Chưa có kiểm định lâm sàng hoặc benchmark trí nhớ cá nhân; cần tích lũy đánh giá để hiệu chỉnh.

## Kiểm thử

Node: đường suy giảm, mốc ôn trước 90%, phản hồi tốt/yếu/exposure, giữ hạn cũ, idempotence và giới hạn chuỗi dài. Chrome: hai bài cùng thư mục vẫn độc lập, bài đến hạn sớm đứng trước dù học sau, marker cũ không chặn ôn, biểu đồ, phản hồi và xếp lại hàng đợi. Java kiểm tra cùng các mốc toán học và giới hạn. CI tiếp tục chạy toàn bộ bộ kiểm thử cũ, build ký khóa cũ và Android 16. Không thể xác nhận dự đoán trí nhớ đúng với từng người chỉ bằng kiểm thử phần mềm.

## Tỷ lệ từ thuộc / từ đã học

Bảng Từ vựng trong bài học hiển thị `Ước tính đã thuộc X / Y từ đã học · Z%` cho từng khóa và tổng chung. Y là số từ gốc không trùng trong các bài đã học xong trong phạm vi đó. X là tổng trọng số % nhớ tự báo cao nhất của các bài chứa từ, làm tròn xuống để tránh báo 100% khi chưa đủ; từ đã lưu nhưng chưa đạt khoảng ôn 21 ngày có trọng số 0, thẻ đạt từ 21 ngày có trọng số 1. Từ không có đánh giá vẫn tính vào Y và được nêu riêng, không tự coi là thuộc. Z tính từ X/Y; Y=0 không hiển thị phần trăm giả. Đây là ước tính dựa trên tự báo/SRS, không xác minh riêng từng từ. Không cộng trùng từ giữa các khóa trong tổng chung; không tính từ chỉ có ở bài chưa học/đang học.

## Dấu học xong

Thư mục thật và thư mục cha ảo có tick trên icon khi tất cả bài trong toàn bộ cây con đã được đánh dấu học xong. Không suy ra từ phần danh sách đang lọc/tìm kiếm, không tick thư mục rỗng. Bỏ đánh dấu bất kỳ bài con nào sẽ bỏ tick cha khi render lại. Bài riêng có tick trước tên, không phụ thuộc bật/ẩn thông tin. Không tự đánh dấu thêm bài hoặc thay đổi dữ liệu học.

Trạng thái ở tên bài/thư mục chỉ có Đã học hoặc Chưa học. Menu chỉ có thao tác đối ứng: đã học → Đánh dấu chưa học; chưa học → Đánh dấu học xong. Áp dụng bài, thư mục thật/ảo (gồm cây con), khóa học và nhãn thao tác chọn nhiều. Bỏ đánh dấu chỉ đổi done/xóa lịch ôn của bài; giữ giây nghe, vị trí và thống kê cũ, không gọi resetLearn. Cơ chế Đang học nội bộ phục vụ hàng phát vẫn giữ nguyên.
