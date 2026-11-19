# Giọng tiếng Việt KorvaTTS trong Deep Flow

- Mô hình: [dogenthq/KorvaTTS](https://huggingface.co/dogenthq/KorvaTTS) — trọng số, giọng mẫu: **Apache-2.0** (xem `KorvaTTS-LICENSE`, `KorvaTTS-NOTICE`).
- Kiến trúc / mã suy luận gốc: Supertonic (Supertone Inc., **MIT**); `KorvaEngine.java` chép lại quy trình của gói Python `korvatts` 0.1.3 (Apache-2.0).
- Chạy bằng ONNX Runtime (Microsoft, **MIT**).
- Thay đổi so với bản gốc: trọng số Conv/MatMul/Gemm được lưu dạng int8 theo từng kênh (DequantizeLinear) để file nhẹ hơn ~3 lần — `tools/korva_prepare.py`.
- File cho app nằm ở GitHub Release `korva-v1` (workflow `KorvaTTS voices`, chạy tay).
- Tác giả mô hình đề nghị: chỉ dùng giọng có sẵn / giọng mình có quyền; không dùng giọng tạo ra để lừa dối, quấy rối, mạo danh.
