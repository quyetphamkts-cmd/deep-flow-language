# Rà soát AI Engine — 27/10/2026

Bản đề xuất: 1.0.3-beta.49. Chỉ sửa luồng API, cache, trạng thái và che khóa; không đổi dữ liệu bài học, từ vựng hay cấu trúc IndexedDB.

## Vì sao app báo hết lượt

- App tự đặt RPM: Gemini 9 (Flash), 14 (Lite); Claude 40, OpenAI text 60, Whisper 20; có thể cấu hình trong settings. Đây là hàng đợi cục bộ, không phải quota tài khoản.
- Nhà cung cấp trả 429: có thể là RPM/TPM, quota ngày, hoặc OpenAI `insufficient_quota` (số dư/hạn mức). Trước đây nhiều trường hợp cùng hiện “hết lượt”. Không thể kết luận tài khoản cụ thể đã hết tiền nếu chưa có phản hồi API thật.
- App trước đây tự chặn thêm 60 giây, Gemini suy đoán giờ reset theo ngày; có thể gây hiểu nhầm về lúc dùng lại.
- Các yêu cầu văn bản giống nhau chạy đồng thời chưa được gộp; alias Gemini có thể gửi lại cùng mô hình.

## Bản sửa

Phân biệt tốc độ, quota ngày, số dư và quá tải; ưu tiên Retry-After (giây/ngày HTTP), retry-after-ms, Gemini retryDelay (kể cả số thập phân). OpenAI/Claude tối đa 4 lần gửi, không thử lại ngay khi hết quota/số dư; Gemini tối đa 5 vòng với danh sách mô hình đã bỏ alias trùng. Thời gian chặn chung được giữ cho các tác vụ khác. Khi không có thời gian reset quota/ngân sách, app chờ 6 giờ trước khi kiểm tra lại, không khẳng định nhà cung cấp sẽ cấp lại quota lúc đó.

Gộp yêu cầu JSON văn bản đang chạy; giữ cache 30 ngày/tối đa 500 mục, khóa cache khớp nội dung và cấu hình mô hình thay cho hash 32 bit. Không cache lỗi; lỗi được giải phóng để lần sau thử lại. Không gộp các yêu cầu âm thanh có callback/cập nhật tiến độ để tránh thay đổi chức năng phụ đề.

## API key

Key do người dùng nhập được lưu trong settings localStorage và đồng bộ qua file riêng `deepflow-keys.json` trong Drive appData của tài khoản. Đây không phải kho khóa được mã hóa; không thay đổi cơ chế lưu để tránh làm mất key/dữ liệu hiện có. Bản sao lưu và đồng bộ settings thường loại bỏ key; đăng xuất gỡ key khỏi máy. Cần bảo vệ thiết bị và tài khoản Google.

Không thêm key vào mã nguồn/CI. Che key đang dùng (kể cả định dạng khác), key Vertex `AQ.`, Authorization và x-api-key trong nhật ký; lọc phản hồi lỗi OpenAI/Claude trước khi hiển thị. Khóa dùng nhận dạng yêu cầu đang chạy chỉ nằm trong RAM, không lưu cache hoặc ghi log.

## Kiểm thử

- 14 ca Node với thời gian/API giả lập: hàng đợi đồng thời, hủy Gemini, retry header, giới hạn số lần thử, quota không lặp, chặn tác vụ sau, gộp yêu cầu, cache, giải phóng lỗi, đổi mô hình, che khóa, Gemini RetryInfo và alias.
- `node --test tests/*.test.cjs`: đạt.
- `node tools/check-web.cjs`: cú pháp cả hai script, phiên bản và changelog khớp.
- `npx cap sync android`: đạt. Hai file Gradle sinh tự động được trả về nguyên trạng, không đưa thay đổi native vào PR.
- Build APK debug đã thử nhưng chưa hoàn tất: không tải được Gradle 8.2.1 do môi trường không mở được kết nối mạng/proxy. Đã thêm workflow PR để kiểm thử, giải mã khóa hiện có, xác nhận alias deepflow và build release ký bằng khóa cũ trên GitHub Actions; chỉ lưu artifact, không phát hành. PR từ fork không dùng secret và chỉ build không ký. Phải xem kết quả thực tế sau khi workflow chạy.
- Chưa gọi API có tính phí, chưa kiểm thử với key/tài khoản thật, chưa chạy trực tiếp trên điện thoại. Không có key thử nghiệm hay thiết bị kết nối trong phiên.

## Khi nào điện thoại được cập nhật

Đẩy nhánh/PR chưa cập nhật điện thoại. Sau khi merge vào main, APP_VER và update/version.json mới được app tải từ raw.githubusercontent.com; app kiểm tra khi mở/quay lại và người dùng xác nhận cập nhật web. Bản sửa chỉ thay web, không cần APK mới.

Workflow build-apk.yml hiện chạy khi đổi native/package/scripts hoặc chạy thủ công; không chạy chỉ vì đổi www/update. Khi cần APK ký để cài đè: chạy workflow Build APK thủ công, có secret DF_SIGN_PASSWORD và tệp khóa mã hóa hiện có. Workflow này phát hành GitHub Release nếu có khóa ký; workflow kiểm tra AI mới build bằng khóa cũ và chỉ lưu artifact, không phát hành. Giữ nguyên khóa và alias; không gỡ app để thử vì có thể mất dữ liệu. Secret DF_SIGN_PASSWORD đã có theo xác nhận của chủ dự án; chưa phát hiện thiếu cấu hình ký, nhưng phải chạy Actions mới xác minh được mật khẩu, alias và ký APK thực tế.
