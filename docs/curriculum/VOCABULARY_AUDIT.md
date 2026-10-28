# Kiểm toán từ vựng và họ từ C01–C12

**Chưa đạt chuẩn 3.000 từ; không phát hành.** Báo cáo tạo từ dữ liệu có thể kiểm tra lại.

## Đơn vị đếm

Mỗi lemma từ đơn là một mục; biến thể không tăng số mục. Từ phái sinh giữ mục riêng và liên kết bằng họ từ. Cụm từ là danh mục riêng, không cộng vào mục tiêu từ đơn. Tên riêng và dạng chữ thô không đại diện cho từ vựng chuẩn.

## Kết quả

- 173 mục đề xuất: 171 từ đơn và 2 cụm; 250 dạng bổ sung khai báo.
- 162 mục có mặt trong core, gồm 160 từ đơn.
- 24 nhóm họ từ nhiều thành viên đề xuất, bao phủ 53 mục; 118 từ đơn chưa phân nhóm. Không gọi các mục chưa phân nhóm là các họ từ đã xác minh.
- 2126 dạng chữ khác nhau và 35446 lượt từ; không dùng làm số lemma.
- 43 mục chỉ xuất hiện trong một bài chính; chưa chứng minh đủ ôn cách quãng.
- 0 mục phân bổ chính thức trong manifest; lần đầu xuất hiện chỉ là đề xuất.

## Theo bài

| Bài | Mục theo dõi có mặt | Lần đầu xuất hiện (đề xuất) | Phân bổ chính thức |
|---|---:|---:|---:|
| C01 | 48 | 48 | 0 |
| C02 | 45 | 23 | 0 |
| C03 | 53 | 19 | 0 |
| C04 | 53 | 15 | 0 |
| C05 | 60 | 16 | 0 |
| C06 | 62 | 17 | 0 |
| C07 | 59 | 7 | 0 |
| C08 | 52 | 9 | 0 |
| C09 | 50 | 3 | 0 |
| C10 | 57 | 2 | 0 |
| C11 | 56 | 3 | 0 |
| C12 | 67 | 0 | 0 |

## Họ từ đề xuất

| Nhóm | Các mục |
|---|---|
| repair | repair, repairable |
| draw | draw, drawing |
| accountant | accountant, account |
| quiet | quiet, quietly |
| privacy | privacy, private |
| measure | measure, measurement |
| choice | choice, choose |
| agree | agree, agreement |
| permission | permission, permit |
| apologize | apologize, apology |
| invite | invite, invitation |
| safe | safe, safety, safely |
| grow | grow, growth |
| responsible | responsible, responsibility |
| patient | patient, patience, patiently |
| reuse | reuse, reusable |
| proud | proud, pride |
| teach | teach, teacher |
| decision | decision, decide |
| compare | compare, comparison |
| comfortable | comfortable, comfort, comfortably |
| maintain | maintain, maintenance |
| care | care, careful, carefully, careless |
| include | include, inclusion |

## Mục không có trong core

ribbon, branch, wing, sparrow, ladder, string, hook, gently, rough, tear, thread. Các mục ở bài mở rộng không bù tiêu chí core.

## Phần còn thiếu

- Chưa nhập danh sách chuẩn 3000 từ có nguồn và giấy phép.
- Bảng họ từ và loại từ là đề xuất, chưa duyệt ngoài.
- Lần đầu xuất hiện chỉ là gợi ý phân bổ, không thay word_ids trong manifest.
- Chưa chứng minh 250 từ mới/bài; không cộng cụm từ hoặc biến thể để bù số.
- Số lần nhắc trong văn bản không chứng minh học thuộc hoặc đạt lịch ôn ngắt quãng.

Các đoạn nguồn cho từng mục được lưu trong JSON xuất bằng cùng CLI. Phép ghép dạng có thể nhận nhầm đồng hình/loại từ (ví dụ permit danh từ/động từ, drawing danh từ/động từ); cần rà soát nghĩa trước khi duyệt từng mục. Tất cả IPA, nghĩa và nhóm hình thái mới là bản biên soạn chờ rà soát.

