# Đọc thử C01/Z01 trên trình duyệt

Bản đọc thử độc lập giúp duyệt nội dung trước khi tích hợp Deep Flow. Không đổi app đang dùng, không có audio hoặc yêu cầu API; không ghi âm, chấm điểm hay lưu tiến độ.

## Mở bản đọc thử

Trong repository, chạy Node 20 trở lên:

```sh
node tools/curriculum/build-preview.cjs > /tmp/deepflow-curriculum-preview.html
```

Mở file HTML vừa tạo bằng trình duyệt. File chứa sẵn nội dung, không cần máy chủ hoặc kết nối mạng. Trên GitHub Actions, tải artifact `curriculum-review`, giải nén rồi mở `curriculum-preview.html`. Trang blob của GitHub chỉ hiển thị mã HTML; cần mở file đã tải xuống để tương tác. Trên điện thoại, khả năng mở file HTML cục bộ tùy trình duyệt/trình quản lý file.

## Thử những gì?

- Chọn C01 hoặc Z01, sau đó chọn phần bài. Bản C01 hiển thị gia đình năm thành viên đã chốt.
- Bật Song ngữ để đọc bản dịch ở các đoạn đã có bản dịch Việt.
- Trong Phản xạ hỏi đáp, đọc câu hỏi và trả lời thành tiếng. Bấm Chờ để thử khoảng suy nghĩ đã biên soạn; có thể tạm dừng/tiếp tục, hoặc tự mở đáp án mẫu.
- Đổi câu/bài sẽ hủy khoảng chờ cũ. Khi trang bị ẩn, khoảng chờ tạm dừng; trở lại trang không tự tiếp tục.
- Xem đoạn truyện để đối chiếu câu hỏi với nguồn. Đáp án mẫu không yêu cầu nói đúng nguyên văn và chưa có chấm tự động.
- Mở bảng từ/cụm đề xuất để xem IPA/nghĩa. Hiện có 48 mục, chưa phải danh sách chuẩn 3.000 từ.

## Kiểm thử

```sh
node --test tests/curriculum*.test.cjs
node tests/curriculum-preview.browser.cjs
```

Kiểm thử trình duyệt cần module Playwright và Chrome; dùng `NODE_PATH`, `CHROME_BIN` theo môi trường. Test tự mở HTTP server chỉ trên loopback, mặc định cổng 31460; đổi qua `PREVIEW_TEST_PORT` nếu cần. Test chặn mọi yêu cầu ra ngoài và không gọi API thật.

Bản HTML dùng chính sách không kết nối mạng, chỉ cho script đi kèm đúng hash chạy. Nội dung nhập được giữ dưới dạng dữ liệu và hiển thị bằng textContent. Đây là biện pháp cho bản duyệt độc lập, không thay cho cơ chế an toàn của app chính.
