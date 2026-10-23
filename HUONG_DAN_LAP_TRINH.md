# Deep Flow Language — hướng dẫn cho người (hoặc AI) cùng lập trình

App Android học ngoại ngữ qua nghe (kiểu Poweramp): phát bài nghe, phụ đề chạy theo âm thanh, song ngữ, tra từ, từ vựng, trò chơi, khoá học, đồng bộ Google Drive.

## Cấu trúc
| Đường dẫn | Vai trò |
|---|---|
| `www/index.html` | **Toàn bộ ứng dụng** (HTML + CSS + JS trong 1 file). Đây là file chính cần sửa. |
| `update/version.json` | Thông tin phiên bản để app tự cập nhật (`ver`, `notes`, `changelog`, `apk.versionCode/versionName`). |
| `android/` | Dự án Capacitor 6.2.2 (appId `vn.vietan.deepflow`). Java: `MainActivity` (giữ phát nền khi vuốt tắt app), `HeadsetPlugin` (trạng thái phát, chế độ phát, widget, chẩn đoán), `GDrivePlugin` (đăng nhập Google / Drive), `DfWidget` (widget màn hình chính), `HeadsetReceiver`. |
| `scripts/patch-media-session.js` | Vá plugin `@jofr/capacitor-media-session` (chạy khi `npm install`): nút chế độ phát / tắt trên thông báo, phụ đề trên màn hình khoá. |
| `data/vocab/` | Danh sách từ theo tần suất cho các ngôn ngữ (wordfreq). |
| `.github/workflows/build-apk.yml` | GitHub Actions build APK khi `android/**`, `package*.json`, `scripts/**` thay đổi; tag phát hành lấy từ `update/version.json` → `apk.versionName`. |

## Trong `www/index.html`
- Có **2 thẻ `<script>`**: script 1 là app chính; script 2 là “cầu nối” chỉ chạy trong APK (`Capacitor.isNativePlatform()`), tự tạo proxy `P(name)` gọi plugin gốc qua `Capacitor.nativePromise / nativeCallback / addListener` (file HTML đứng một mình nên không có `@capacitor/core`).
- Cài đặt: object `S` lưu ở `localStorage['engamp']`; từ vựng ở `localStorage['dfl_vocab']`; bài học (meta + âm thanh) ở IndexedDB `engamp`.
- Các khối quan trọng (tìm theo tên hàm): `renderLib` (Thư viện, khoá học, Ôn tập), `units/listOf` (album / bài lẻ thuộc khoá học), `albumInfo` (trạng thái tự động Đang học / Chưa học / Đã học xong), `buildCuesFrom / tidyRaw / checkSubs / subSweep` (phụ đề và tự kiểm tra), `runAuto / batchSubs / gemTranscribe / oaTranscribe` (AI tạo phụ đề), `aiJSON / gemPost / aiFetch` (gọi Claude / ChatGPT / Gemini có giới hạn tốc độ, thử lại, cache), `trGemini` (dịch), `renderVoc / lessonWords / GAMES` (từ vựng, trò chơi), `checkUpdate / applyUpdate` (tự cập nhật).
- Chữ hiển thị và chú thích code bằng **tiếng Việt**.

## Quy ước khi sửa và phát hành
1. Sửa `www/index.html`.
2. Tăng `APP_VER` (dạng `1.0.1-beta.N`), thêm 1 mục vào đầu mảng `CHANGELOG` trong HTML **và** vào `update/version.json` (`ver`, `notes`, `changelog`). Giữ `"web":"1.0.1"`.
3. Nếu sửa phần Android (`android/**`): tăng thêm `apk.versionCode` và `apk.versionName` trong `update/version.json` để CI build APK mới.
4. Kiểm tra cú pháp: tách 2 thẻ `<script>` ra file rồi chạy `node --check`.
5. Commit + push lên nhánh `main` → app trên điện thoại tự tải bản mới (không cần cài lại APK, trừ khi đổi phần Android).

## Không được đưa vào mã nguồn
- API key (Gemini / Claude / ChatGPT / Google Drive) — chỉ nhập trong app, lưu riêng theo tài khoản Google.
- File ký APK (`*.jks`) và mật khẩu ký — giữ riêng; bản nén này đã bỏ file keystore mã hoá.

## Lưu ý khi 2 bên cùng sửa
Mỗi lần bắt đầu, hãy lấy bản mới nhất từ GitHub (`git pull`) — nhiều người / nhiều phiên AI cùng đẩy lên dễ ghi đè nhau. Số beta phải tăng dần, không trùng.

**Quy tắc đánh số phiên bản:** beta tăng dần `1.0.1-beta.1` … `1.0.1-beta.99`. Sau beta 99 thì tự lên phiên bản: bản kế tiếp là bản chính thức `1.0.1` (gộp các thay đổi), rồi bắt đầu chu kỳ mới `1.0.2-beta.1` … `1.0.2-beta.99` → `1.0.2`, cứ thế tiếp. Hàm `vparse` trong HTML đã xếp đúng thứ tự: `1.0.1-beta.99` < `1.0.1` < `1.0.2-beta.1`. Khi lên số mới nhớ đổi cả `"web"` trong `version.json`.
