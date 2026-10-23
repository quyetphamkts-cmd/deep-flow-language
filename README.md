# Deep Flow Language

Ứng dụng nghe ngoại ngữ kiểu Poweramp: phụ đề chạy theo âm thanh, song ngữ, AI tạo phụ đề, luyện nói chấm phát âm, hộp từ vựng flashcard, đồng bộ Google Drive.

- `www/index.html` — toàn bộ ứng dụng (một file). App cài trên điện thoại tự tải bản mới của file này, **không cần cài lại APK**.
- `update/version.json` — thông tin phiên bản để app tự kiểm tra cập nhật.
- `android/` — phần lõi Android (Capacitor 6): chạy nền + điều khiển màn hình khoá, tai nghe, đăng nhập Google.
- `.github/workflows/build-apk.yml` — GitHub tự build file APK và đăng ở mục **Releases**.

## 1. Thêm mật khẩu khoá ký (làm 1 lần)

Khoá ký APK được lưu ở `signing/` dưới dạng **đã mã hoá**. GitHub cần mật khẩu để giải mã khi build:

1. Vào **Settings → Secrets and variables → Actions → New repository secret**.
2. **Name:** `DF_SIGN_PASSWORD` — **Secret:** mật khẩu đã được gửi riêng cho bạn.
3. Bấm **Add secret**.
4. Vào tab **Actions → Build APK → Run workflow** (hoặc bấm *Re-run* lần build bị lỗi).

> ⚠️ Giữ kỹ mật khẩu này (và file `deepflow-release.jks`). Mất nó thì các bản sau không cài đè được lên bản cũ.

## 2. Cài APK

Mở **Releases → DeepFlowLanguage.apk** trên điện thoại → tải về → mở file → cho phép “Cài ứng dụng không rõ nguồn gốc” → **Cài đặt**.
Lần sau có bản mới: tải APK mới và cài **đè** lên (không gỡ app cũ — gỡ sẽ mất dữ liệu).

## 3. Cho phép đăng nhập Google (để đồng bộ Drive)

Trong **console.cloud.google.com**, chọn project bạn đã dùng cho API key Google Drive:

1. **APIs & Services → Library** → bật **Google Drive API** (nếu chưa bật).
2. **OAuth consent screen** (Google Auth Platform) → *Get started*:
   - App name: `Deep Flow Language`, User support email: email của bạn.
   - Audience: **External**. Contact email: email của bạn → **Create**.
   - Mục **Audience** → **Test users → Add users** → thêm email Gmail bạn sẽ đăng nhập trong app.
3. **Clients → Create client**:
   - Application type: **Android**
   - Package name: `vn.vietan.deepflow`
   - SHA-1 certificate fingerprint: `A3:E5:70:04:44:07:18:AB:BF:42:CC:6B:29:D4:4E:4D:93:64:25:DB`
   - **Create**.
4. Mở app → **Cài đặt → Tài khoản & đồng bộ → Đăng nhập Google**. Nếu Google báo “ứng dụng chưa được xác minh”, bấm **Nâng cao → Tiếp tục** (app của chính bạn).

App chỉ dùng 2 quyền: thư mục ẩn riêng của app (`drive.appdata`) và các file do app tạo (`drive.file`) — không đọc được các file khác trong Drive của bạn.

## 4. Phát hành bản mới

- **Chỉ đổi giao diện / tính năng web:** sửa `www/index.html`, tăng `APP_VER` trong file đó và `"ver"` trong `update/version.json` → app trên điện thoại tự báo “Có cập nhật”.
- **Cách đánh số:** bản thử nghiệm là `x.y.z-beta.N` (vd `1.0.1-beta.1`, `1.0.1-beta.2`…), mỗi lần sửa chỉ tăng N. Khi ổn định thì phát hành bản chính thức `x.y.z` (vd `1.0.1`) — bản chính thức luôn mới hơn mọi bản beta cùng số.
- Trường `"web"` giữ dạng số thường (vd `1.0.1`) chỉ để APK bản đầu (giao diện 1.0.0) nhận ra có cập nhật; app từ 1.0.1-beta.1 trở đi đọc `"ver"`.
- **Đổi phần lõi Android:** tăng `apk.versionCode` và `apk.versionName` trong `update/version.json` (và `minNative` nếu bản web mới cần lõi mới) → GitHub tự build APK mới.
