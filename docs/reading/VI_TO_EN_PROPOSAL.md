# Báo Việt → tiếng Anh và rà soát ngôn ngữ

Trạng thái: đánh giá theo câu hỏi của người dùng, chưa triển khai hoặc gọi API. Không ghép âm thầm vào phần sửa giao diện beta.57 khi chưa chốt phạm vi.

## Hiện trạng đã kiểm tra

`nbTrLines` dùng `nbLg()` (ngôn ngữ học) làm nguồn và `S.trLang || 'vi'` làm đích, thử Google Dịch rồi AI dự phòng. `nbTrArt` lưu map văn bản nguồn → bản dịch và ưu tiên phần quanh trang đang đọc. Chưa có nhận diện nguồn Việt riêng hoặc trạng thái rà chính tả/ngữ pháp. Không thể gọi đây là tính năng Việt → Anh đã hoàn thiện.

## Tích hợp đề xuất

- Mặc định bấm Dịch để tạo bản Anh và rà soát; chỉ tự động sau khi người dùng bật tùy chọn. Cần mạng; AI có thể tiêu tốn quota/chi phí theo tài khoản đã cấu hình. Không gọi dịch vụ trả phí trong lúc triển khai/kiểm thử nếu chưa được phê duyệt.
- Kết hợp `html lang` và nội dung để đề xuất tiếng Việt; không suy ra chỉ từ đuôi tên miền `.vn`. Nếu không chắc, cho chọn nguồn. Giữ bản Việt gốc và URL nguồn bất biến, tách rõ bản dịch/bản AI rà soát.
- Bản Anh là văn bản đọc học chính; song ngữ theo từng cặp câu Anh–Việt, có thể quay về bản gốc. Từ điển, IPA, thống kê từ, TTS và sao chép phải dùng đúng ngôn ngữ đang hiển thị. Không biến việc rút gọn/viết lại theo CEFR thành bản dịch nguyên gốc.
- Dịch theo nhóm câu có ID ổn định rồi rà chính tả/ngữ pháp, đồng thời kiểm tra tên riêng, số liệu, ngày tháng, đơn vị, phủ định và trích dẫn. Khóa cấu trúc bằng ID/số lượng; không cho trả thêm đoạn giải thích trộn vào bài đọc. Nội dung bài báo là dữ liệu, không phải chỉ dẫn cho AI.
- Hiển thị trung thực: bản gốc / đang dịch / đã dịch / đang rà soát / đã được AI rà soát / chưa rà soát hoặc có lỗi. Không hứa đúng tuyệt đối hoặc tự sửa nguồn của nhà xuất bản.
- Dùng hàng đợi/cache/chống trùng/thử lại có giới hạn của AI Engine hiện có. Cache phân biệt hash nguồn, cặp ngôn ngữ, phiên bản nội dung, model và chính sách rà soát; lưu từng khối thành công và hủy cập nhật màn hình khi đổi bài. Không gửi lại toàn bộ bài mỗi lần lật trang.
- Nếu không có AI phù hợp hoặc hết quota, giữ bản gốc/bản dịch đã có, báo lý do; không ghi trạng thái đã rà soát giả. Xác định hạn mức xử lý mỗi bài trước khi bật tự động.

## Kiểm thử cần có khi được chọn triển khai

Bài Việt/Anh và trang `.vn` có nội dung Anh; nhận diện không chắc; dấu câu và song ngữ đúng cặp; copy/tra từ/thống kê đúng ngôn ngữ; số liệu/tên riêng và ID không bị đổi; lỗi 429/hết quota; đổi bài giữa yêu cầu; cache ngăn gọi trùng; khôi phục bản gốc; không có lời gọi API thật trong kiểm thử.
