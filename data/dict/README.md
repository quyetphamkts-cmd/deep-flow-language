# Từ điển offline của Deep Flow

App tải 2 file này **một lần** (khi có Wi-Fi) rồi tra từ không cần mạng.

| File | Nội dung | Nguồn | Giấy phép |
|---|---|---|---|
| `en-vi.json.gz` | Anh – Việt: ~39.700 từ thông dụng + ~14.400 cụm từ — phiên âm, từ loại, nghĩa, ví dụ kèm bản dịch, thành ngữ | Free Vietnamese Dictionary Project (Hồ Ngọc Đức và cộng sự, 1997–2004), http://www.informatik.uni-leipzig.de/~duc/Dict/ — bản trên GitHub: robert-strandh/VietAnh | GNU GPL v2 trở lên. File này là dữ liệu trích từ cơ sở dữ liệu FVDP, phát hành lại theo GPL |
| `en-en.json.gz` | Anh – Anh: ~51.800 từ — định nghĩa, ví dụ, từ đồng nghĩa | Princeton WordNet 3.0 | Giấy phép WordNet 3.0 (xem `WORDNET-LICENSE.txt`) |

Chọn từ theo danh sách tần suất `wordfreq` (80.000 từ thông dụng nhất + dạng gốc; Anh – Anh lấy các từ có trong bản Anh – Việt + 40.000 từ đầu). Cụm từ: mục nhiều chữ của FVDP và thành ngữ / cụm động từ trong mục của từ chính (“!to give up” → “give up”).
Tạo lại bằng `scripts/build_dict.py` (cần `pip install wordfreq nltk`, dữ liệu WordNet của NLTK và file `anhviet.dict` của FVDP).

Định dạng (JSON nén gzip):
- `en-vi`: `{ "từ": [ipa, [[từ_loại, [[nghĩa, [[ví_dụ_Anh, ví_dụ_Việt], …]], …]], …], [[thành_ngữ, nghĩa], …]?] }`
- `en-en`: `{ "từ": [[pos, [[định_nghĩa, ví_dụ?, [đồng_nghĩa…]?], …]], …] }`
