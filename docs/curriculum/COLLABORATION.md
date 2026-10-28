# Phân công ChatGPT và Codex

Ngày lập: 28/10/2026. Repository chung: `quyetphamkts-cmd/deep-flow-language`.
Tài liệu này bổ sung quy trình làm việc; sáu tệp bàn giao gốc trong ZIP được giữ nguyên.

**Điều chỉnh đã được người dùng đồng ý ngày 28/10/2026:** trong khi phiên ChatGPT Work chưa có công cụ ghi GitHub khả dụng, Codex được đảm nhận cả biên soạn nháp C01/Z01 lẫn kỹ thuật, dùng tài liệu hiện có làm chuẩn. Nội dung do Codex viết vẫn là bản nháp chờ người dùng duyệt; không đại diện cho một lần ChatGPT khác đã duyệt. Có thể quay về phân công hai bên dưới đây khi công cụ kết nối được xác minh. Không bật tự động hóa API hoặc phát hành theo điều chỉnh này.

## Mỗi đầu việc có một bên phụ trách

| Đầu việc | Phụ trách chính | Bên kiểm tra | Sản phẩm bàn giao |
|---|---|---|---|
| Cốt truyện 12 bài chính, thế giới 12 con giáp, nhân vật | ChatGPT | Người dùng | Story Bible và đề cương có phiên bản |
| Tìm nguồn danh sách từ, giải thích lựa chọn và quyền sử dụng | ChatGPT đề xuất; người dùng chốt nguồn | Codex kiểm tra định dạng, trùng và giấy phép có được khai báo | Danh sách chuẩn, URL nguồn, giấy phép, cách tính lemma |
| Chọn từ mục tiêu/ôn lại, từ khó cần Word Discovery | ChatGPT | Codex đếm; người dùng duyệt phương án | Phân bổ từ có ID ổn định |
| Viết chuyện, câu hỏi/đáp án, Time Shift, dịch | ChatGPT | Người dùng duyệt nội dung; Codex kiểm tra dữ liệu | Scripts JSON kèm nguồn, phiên bản và lý do chỉnh sửa |
| Schema, nhập dữ liệu, kiểm tra từ, báo cáo lỗi | Codex | Kiểm thử tự động và ChatGPT xem lỗi nội dung | Công cụ CLI và kết quả kiểm thử |
| TTS, cache, hàng đợi, chi phí, đồng bộ audio/phụ đề | Codex | Người dùng duyệt mẫu giọng và ngân sách | Mock trước; audio thật chỉ sau khi được phép |
| Giao diện nghe/đọc, lưu tiến độ, lịch ôn | Codex | Người dùng thử trên điện thoại | PR kỹ thuật và báo cáo tương thích dữ liệu |
| Duyệt nội dung cuối, bật chi phí, phát hành | Người dùng | Hai bên cung cấp kết quả kiểm tra | Quyết định phê duyệt rõ phạm vi |

ChatGPT sửa nội dung; Codex báo lỗi nội dung bằng ID bài/đoạn và đề xuất, không tự viết lại bản đã duyệt. Codex sửa công cụ; ChatGPT không sửa player, Android, signing hoặc workflow. Mỗi PR chỉ có một chủ sở hữu.

## Dùng GitHub để không phải chuyển file

1. Đưa bộ tài liệu và công cụ lên GitHub sau khi người dùng duyệt lần đầu.
2. Cấp quyền truy cập đúng repository cho cả phiên ChatGPT và Codex. Muốn ChatGPT tự bàn giao nội dung, công cụ của phiên đó phải hỗ trợ **tạo/sửa file, nhánh và PR**. Kết nối chỉ đọc hoặc đọc file trong Project không đủ. Kiểm tra khả năng thực tế trước khi dựa vào nó.
3. Nội dung được làm trên nhánh `content/C01-v1`, sau đó `content/Z01-v1`; kỹ thuật trên `feat/curriculum-*`. Bắt đầu mỗi lượt bằng đọc nhánh/PR và phiên bản mới nhất; không ghi đè toàn bộ tài liệu từ bản cũ.
4. ChatGPT mở PR nội dung. Workflow `curriculum-checks.yml` kiểm tra tự động và tạo artifact `curriculum-review` gồm báo cáo từ vựng và bản xem trước. Workflow này không gọi AI, không sửa chuyện, không tạo giọng đọc hoặc triển khai app.
5. Codex đọc PR và báo cáo, sửa công cụ nếu là lỗi kỹ thuật hoặc ghi danh sách lỗi nội dung trong PR để ChatGPT sửa. ChatGPT đọc lại PR ở lượt làm việc kế tiếp. Mọi thay đổi có bản diff để xem và hoàn tác.
6. Người dùng duyệt khi nội dung và kỹ thuật đạt yêu cầu. Nhánh giáo trình không được tự merge vào `main`: app hiện lấy bản cập nhật từ `main`.

**Giới hạn:** hai cuộc trò chuyện không tự đọc ngữ cảnh hoặc tự đánh thức nhau. Chia sẻ GitHub giúp bỏ thao tác gửi file, nhưng vẫn cần khởi động lượt làm việc của mỗi bên nếu chưa có bộ điều phối. Không thể biến một phiên ChatGPT thông thường thành tác vụ tự chạy chỉ bằng cách ghi hướng dẫn trong repository.

**Nếu muốn vòng lặp tự động hoàn toàn:** cần một bộ điều phối có quyền đọc/ghi GitHub, gọi mô hình qua API, chuyển lỗi QA về lượt sửa tiếp theo và tạo PR. Phải đặt giới hạn số vòng sửa, chi phí, thời gian, retry và tách quyền khỏi signing/production. Đây là phần riêng, chưa triển khai, chưa gọi API và chưa được tính là đã kết nối. Không chuyển API key qua chat hoặc lưu vào repository.

## Hai việc đầu tiên

- **ChatGPT:** hoàn thiện bản nháp C01 và Z01, trước hết đề cương/nhịp chuyện, rồi từng phần audio. Đề xuất nguồn từ chuẩn và cách xử lý đa nghĩa/cụm từ; chưa công bố đủ 3.000 từ. Dùng `curriculum/scripts.placeholder.json` làm mẫu cấu trúc, không đánh dấu `approved` thay người dùng.
- **Codex:** hoàn thiện schema/QA, đối chiếu từ và mẫu C01/Z01 offline; sau đó mới tích hợp trình phát hiện có. Không bật lại cơ chế dừng Mini Story của các khóa đang dùng. Chế độ tương tác mới, nếu duyệt, chỉ áp dụng riêng phần Quick Response của giáo trình mới.

## Quy tắc bàn giao nội dung

- Giữ nguyên ID `C01`…`C12`, `Z01`…`Z60` và liên kết mỗi nhóm năm bài với một bài chính.
- Chỉ tăng `version` của script khi sửa. Ghi lý do thay đổi trong mô tả PR; cập nhật Story Bible nếu thay nhân vật/chi tiết xuyên suốt.
- Mỗi PR ghi bài/phiên bản đã sửa, nguồn tham khảo, thay đổi từ mục tiêu, câu hỏi còn vướng và trạng thái người dùng đã duyệt hay chưa.
- Thời lượng mục tiêu khác thời lượng đo thật. Mock là file im lặng, không phải audio hoàn chỉnh.
- Dữ liệu xuất hiện từ khác bằng chứng người học đã biết từ. Không tự ghi tiến độ hoặc thay lịch SRS đang có trong app.

## Yêu cầu có thể dùng cho phiên ChatGPT

> Đọc `docs/curriculum/COLLABORATION.md`, `CODEX.md`, `PROJECT_BRIEF.md`, `STORY_BIBLE.md`, `TECHNICAL_SPEC.md` và manifest trong repository `quyetphamkts-cmd/deep-flow-language`. Kiểm tra công cụ của bạn có thể ghi file và mở PR không; nếu chỉ đọc thì báo rõ. Bạn phụ trách nội dung C01/Z01; Codex phụ trách schema, QA và tích hợp. Chỉ sửa trên nhánh nội dung riêng, bàn giao qua PR, không sửa mã ứng dụng, tự duyệt, phát hành hoặc dùng API trả phí. Đọc lỗi QA trên PR và sửa đúng phiên bản, không tự tuyên bố đủ 3.000 từ.
