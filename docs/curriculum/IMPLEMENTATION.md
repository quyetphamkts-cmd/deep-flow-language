# Nền tảng giáo trình: phạm vi và cách kiểm tra

## Kiến trúc đã kiểm tra

- Ứng dụng HTML/CSS/JavaScript trong `www/index.html`, đóng gói Capacitor 6.2.2 / Java Android. File web tự cập nhật từ `main`; thay bản beta là thao tác phát hành thật.
- IndexedDB `engamp`: bài học `meta` và audio. `loadTrack`, `renderLib`, phụ đề dạng cue và thư mục/khóa học đã có; sẽ tái sử dụng ở giai đoạn tích hợp.
- Từ học của người dùng ở `dfl_vocab`, settings ở `engamp` localStorage. Flashcard đã có `nextIvl`, `fcDueQueue`, `gradeCard`. Bộ đếm từ giáo trình được tách khỏi lịch ôn hiện tại, chưa có migration dữ liệu.
- Từ điển `dfl_dict`/`dfl_off`; danh sách tần suất `WF_EN`, `data/vocab/*` và xử lý dạng từ đã có. Không coi các danh sách này là danh sách chuẩn 3.000 mục từ được duyệt hoặc đã giải quyết quyền phân phối.
- AI hiện có `aiFetch`, `gemPost`, `aiJSON`, `runAuto` và xử lý giới hạn/retry/cache. TTS phát từ đang có qua Capacitor TextToSpeech và trình duyệt; việc tạo/phiên bản hóa bộ audio giáo trình hàng loạt là phần mới. Chưa nối công cụ giáo trình vào nhà cung cấp thật.
- CI cũ kiểm thử Node, Chrome, Java, emulator và build APK ký. Workflow giáo trình mới chỉ chạy Node và xuất báo cáo; không nhận secret, không ký hay phát hành.

## Các tệp mới

- `docs/curriculum/`: sáu tệp ZIP gốc không chỉnh sửa, bản dán `HANDOFF_V1.md` để lưu nguồn và tài liệu quy trình bổ sung.
- `curriculum/manifest.schema.json`: JSON Schema draft 2020-12 cho định dạng manifest bàn giao v1; kiểm tra liên kết/thứ tự bằng quy tắc bổ sung.
- `curriculum/manifest.cjs`: validator offline cho các keyword được schema này sử dụng (`type`, `required`, `properties`, `additionalProperties`, `const`, `enum`, `pattern`, `minLength`, `items`, `minItems`, `maxItems`, `uniqueItems`, `anyOf`). Không phải bộ triển khai tổng quát mọi keyword JSON Schema. Khi mở rộng schema phải cập nhật validator và kiểm thử cùng PR.
- `curriculum/engine.cjs`: chỉ mục lemma/forms/cụm từ khai báo rõ, kiểm tra phân bổ và độ bao phủ, thống kê xuất hiện mới/ôn lại, từ chưa xuất hiện/ngoài danh sách, lịch ôn bằng chứng độc lập để thử nghiệm. Các phép tính là hàm thuần, chưa ghi vào app.
- `curriculum/scripts.placeholder.json`: sáu thành phần C01/Z01, segments trống, trạng thái draft; không có kịch bản do Codex sáng tác.
- `curriculum/vocabulary.pending.json`: danh sách trống, chưa duyệt, thiếu nguồn/giấy phép; chủ ý báo chưa sẵn sàng.
- `curriculum/pipeline.cjs`: xem trước cấu trúc C01/Z01; mock tạo WAV im lặng cho dữ liệu kiểm thử. Kế hoạch cache hash nội dung/phiên bản/nhà cung cấp/model/voice/language/speed/pauses; chưa có kho audio cache bền vững, adapter thật hay cơ chế chi tiêu.
- `tools/curriculum/check.cjs`, `tests/curriculum.test.cjs`, `.github/workflows/curriculum-checks.yml`: CLI, kiểm thử và CI offline.

Không sửa `www/`, `android/`, signing, package dependencies hoặc `update/version.json`. Không tăng beta vì chưa thay hoặc phát hành ứng dụng.

## Lệnh kiểm tra

Chạy tại thư mục repository, Node 20 trở lên, không cần `npm install` cho công cụ này:

```sh
node --test tests/curriculum.test.cjs
node --test tests/*.test.cjs
node tools/check-web.cjs
node tools/curriculum/check.cjs
node tools/curriculum/check.cjs --preview
node tools/curriculum/check.cjs --require-vocabulary-ready
```

Lệnh cuối hiện **trả mã 2 đúng dự kiến**: danh sách 3.000 từ còn thiếu. Mã 0 ở lệnh mặc định chỉ có nghĩa cấu trúc hợp lệ, không phải giáo trình đã hoàn thành. Lỗi dữ liệu trả mã 1. Có thể đưa file riêng qua `--manifest PATH`, `--vocabulary PATH`, `--scripts PATH`. CLI chỉ đọc và in báo cáo ra stdout, không tự tạo file hoặc ghi đè nội dung.

## Quy ước dữ liệu biên soạn bước đầu

Manifest gốc giữ nguyên schema v1. Bộ chuẩn hóa chỉ tạo cấu trúc nội bộ trong bộ nhớ, không sửa tên bài hay tự đổi phạm vi. Bản script có `schemaVersion:1`, `id` dạng `C01-main_story`, `lessonId`, `component`, `version`, `status` và `segments`.

Mỗi segment có ID ổn định, loại `narration`/`question`/`answer`/`pause`. Đoạn lời có `text`; `pause` có `seconds` >0 và <=120. `question` có `text` là câu hỏi, `answerId` trỏ đến segment `answer` trong phần đó và `answerPauseMs` từ 0 đến 120000. Bản được duyệt phải có nội dung, `approvedBy`, `approvedAt`. Máy chỉ kiểm tra thông tin phê duyệt có mặt; quyết định duyệt vẫn cần người dùng, không phải bằng chứng tin cậy chỉ vì JSON ghi `approved`.

Danh sách chuẩn có `schemaVersion:1`, `version`, `status`, `source`, `license`, `words`. Mỗi từ có `id`, `lemma`, `forms:[]`. Ví dụ thuần kỹ thuật nằm trong test; không đưa vào danh sách từ thật. Có thể bổ sung POS, IPA, nghĩa và yếu tố khó nhớ sau khi chốt nguồn/schema. Dạng từ mơ hồ sẽ bị từ chối để người biên soạn giải quyết, không tự gán `saw` vào một nghĩa. Không tự tách dạng sở hữu hoặc mở rộng contraction. Phân tích cụm từ hiện ghép liên tiếp dài nhất, không ghép qua dấu câu; chưa hỗ trợ phrasal verb tách rời và phân giải nghĩa theo ngữ cảnh.

`vocabularyReady` yêu cầu catalog được khai báo duyệt, đủ đúng 3.000 mục với source/license, mỗi bài chính được phân bổ 250 mục không trùng, các từ xuất hiện trong bài chính đã phân bổ và toàn bộ phần chính phủ đủ từ. Bài mở rộng không bù cho từ thiếu trong phần chính. Đây chỉ là kiểm tra từ vựng, chưa có nghĩa đã đạt chất lượng nội dung, bản quyền hoặc có audio thật. Báo cáo phân biệt `missingScripts` và `emptyScripts`.

Lịch ôn thử nghiệm giữ riêng ba kỹ năng recognition/understanding/use. Phản hồi good: 1–3–7 ngày, again: 10 phút, ưu tiên từ hay quên trong các mục đã đến hạn. Chưa thay thế thuật toán Flashcard của app; bằng chứng hiện chưa có persistence, xác thực người học hoặc hợp nhất sự kiện đa thiết bị.

## Trình tự tiếp theo

1. Chốt quy trình GitHub trong `COLLABORATION.md`; người dùng duyệt commit/push trước khi đưa thay đổi lên repository.
2. ChatGPT bàn giao đề cương và script C01/Z01 có phiên bản; người dùng chốt nguồn danh sách từ và chính sách lemma/đa nghĩa/cụm từ. Không buộc nhồi đủ 250 từ nếu làm câu chuyện khó nghe; thay phạm vi phải được duyệt.
3. Bổ sung liên kết câu hỏi đến câu chuyện, tiêu chí Word Discovery và QA nội dung; máy không thể tự chứng minh câu trả lời đúng, truyện nguyên bản hay hấp dẫn.
4. Sau khi duyệt ngân sách và giọng mẫu, xây adapter TTS thật với hàng đợi, retry có giới hạn, trần chi phí và resume/cache bền vững. Đo thời lượng thật gồm pause; chính sách cảnh báo ngoài 30–45 phút và dung sai bài khoảng 15 phút cần chốt trước khi khóa cổng xuất bản.
5. Xuất cue/phụ đề từ audio thật; nhập riêng vào thư viện bằng ID ổn định, không ghi đè khóa cũ. Kiểm thử nghe từng phần, resume, đọc/nghe và lịch ôn trước khi phát hành có kiểm soát.

Phần này chưa hoàn thành 72 kịch bản, chưa tạo giọng đọc, chưa tích hợp UI, chưa đo độ hiệu quả học hay thời lượng audio thật. Không cần build APK cho thay đổi chỉ ở tài liệu/công cụ; cần kiểm thử Android khi bắt đầu chạm tích hợp player/native.

## Kết quả tại workspace ngày 28/10/2026

- Nhánh `feat/curriculum-foundation`, dựa trên nội dung beta.53 đã phát hành. Tại lần kiểm thử cục bộ này chưa commit/push hoặc chạy CI GitHub; người dùng sau đó đã duyệt commit/push và mở PR, chưa duyệt merge/phát hành.
- `node --test tests/*.test.cjs`: **57/57 đạt**, gồm 16 kiểm thử giáo trình mới và 41 kiểm thử ứng dụng có sẵn. Runner có tiến trình con phải chạy ngoài sandbox hạn chế IPC; lần chạy đầy đủ đã đạt.
- `node tools/check-web.cjs`: đạt, hai script và version/changelog beta.53 khớp.
- CLI mặc định: manifest hợp lệ; vocabularyReady=false, 0/3000 từ, 162 script thiếu và 6 script trống của C01/Z01. Lệnh bắt buộc sẵn sàng từ vựng trả mã 2 đúng dự kiến; preview trả sáu phần chờ nội dung, audio/subtitles=null.
- Sáu tệp gốc được đối chiếu byte với ZIP, không sửa. Không thay file đang được app sử dụng, không gọi API AI/TTS thật, không phát hành.
- Không chạy lại Chrome/Android/build APK ở lượt này vì không thay đổi app hoặc native. Mock WAV đã được kiểm tra định dạng/header bằng test, chưa nghe giọng thật vì mock chỉ im lặng.
