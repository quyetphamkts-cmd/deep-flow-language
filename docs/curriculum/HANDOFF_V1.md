# DEEP FLOW LANGUAGE
## THE ARCHITECT'S JOURNEY & THE TWELVE ZODIAC ADVENTURES
### PROJECT HANDOFF — VERSION 1.0

Bạn là kỹ sư phần mềm phụ trách phối hợp với ChatGPT phát triển hệ thống giáo trình tiếng Anh cho ứng dụng Deep Flow Language.

Repository dự án:
`quyetphamkts-cmd/deep-flow-language`

## 1. MỤC TIÊU DỰ ÁN

Xây dựng hệ thống giáo trình tiếng Anh gồm 72 bài học, sử dụng phương pháp nghe hiểu, kể chuyện, luyện phản xạ và ôn tập ngắt quãng.

Giáo trình gồm hai phần:

**A. The Architect's Journey**

12 bài học chính, kể một câu chuyện xuyên suốt về hành trình cuộc sống, gia đình và sự nghiệp của một kiến trúc sư Việt Nam.

**B. The Twelve Zodiac Adventures**

60 câu chuyện mở rộng, chia thành 12 nhóm tương ứng với 12 con giáp Việt Nam, mỗi nhóm 5 câu chuyện.

Các câu chuyện được sáng tác mới hoàn toàn, có thể lấy cảm hứng từ văn hóa và truyện dân gian Việt Nam nhưng không sao chép các phiên bản hiện đại còn bản quyền.

## 2. CẤU TRÚC BÀI HỌC CHÍNH

Mỗi Core Lesson gồm bốn audio:

1. Main Story — Câu chuyện chính.
2. Word Discovery — Giải thích những từ khó ghi nhớ.
3. Quick Response — Luyện phản xạ hỏi đáp.
4. Time Shift — Kể lại câu chuyện với những thay đổi về thời gian hoặc góc nhìn.

Thời lượng toàn bộ mỗi bài: 30–45 phút.

Quick Response là thành phần ưu tiên về thời lượng.

Word Discovery chỉ giải thích những từ được đánh giá là khó nhớ, không giải thích toàn bộ từ mới.

## 3. CẤU TRÚC BÀI MỞ RỘNG

Mỗi Zodiac Adventure gồm:

1. Daily Adventure — Câu chuyện phiêu lưu.
2. Quick Response — Luyện phản xạ.

Thời lượng khoảng 15 phút mỗi bài.

Bài mở rộng là tùy chọn. Người học có thể nghe, vừa nghe vừa đọc hoặc chỉ đọc.

## 4. HỆ THỐNG TỪ VỰNG

Mục tiêu: 3.000 từ tiếng Anh thông dụng.

Phân bổ dự kiến khoảng 250 từ mục tiêu mới cho mỗi bài chính.

Yêu cầu xây dựng Vocabulary Engine có khả năng:

- Quản lý danh sách 3.000 từ chuẩn.
- Theo dõi lần xuất hiện đầu tiên của mỗi từ.
- Phân biệt từ mới và từ ôn tập.
- Thống kê tần suất xuất hiện trong các bài học.
- Phát hiện từ bị bỏ sót.
- Phát hiện từ ngoài danh sách.
- Quản lý các dạng biến đổi của từ.
- Theo dõi khả năng nhận biết, hiểu và sử dụng từ.
- Đề xuất lịch ôn tập ngắt quãng.
- Ưu tiên những từ người học thường quên.

Không được coi việc từ xuất hiện một lần trong kịch bản là bằng chứng người học đã ghi nhớ từ đó.

## 5. NGUYÊN TẮC SÁNG TÁC

ChatGPT phụ trách sáng tác nội dung.

Áp dụng ba nhóm nguyên tắc:

**Storytelling**

- Nhân vật có mục tiêu rõ ràng.
- Có khó khăn, xung đột và hậu quả.
- Diễn biến có quan hệ nguyên nhân – kết quả.
- Có tình tiết bất ngờ nhưng hợp lý.
- Nhân vật phải đưa ra lựa chọn.
- Kết thúc có ý nghĩa và thể hiện sự thay đổi.

**Memory Design**

- Trí tưởng tượng sống động.
- Hình ảnh nổi bật.
- Liên tưởng từ vựng.
- Tình huống tương phản.
- Kết nối cảm xúc.
- Lặp lại trong nhiều ngữ cảnh.
- Gợi nhớ chủ động và ôn tập ngắt quãng.

**Language Learning**

- Tiếng Anh đơn giản, tự nhiên.
- Phù hợp trình độ người học.
- Câu ngắn, dễ nghe.
- Ưu tiên nghe hiểu và phản xạ.
- Không nhồi nhét từ vựng.
- Nội dung phải hấp dẫn khi nghe lặp lại.

## 6. DANH MỤC 12 BÀI CHÍNH

C01 — A Question at Sunrise

C02 — The House That Listened

C03 — The Missing Drawing

C04 — A Village Without Walls

C05 — The Garden in the Sky

C06 — The Man Who Built Nothing

C07 — The Journey Beyond the Mountains

C08 — The City That Never Slept

C09 — When Everything Went Wrong

C10 — The Smallest House in the World

C11 — A Gift for the Future

C12 — The House of Tomorrow

Đây là tên và đề cương dự kiến, có thể điều chỉnh trong quá trình biên kịch.

## 7. DANH MỤC 60 BÀI MỞ RỘNG

**Tý — Mouse**

Z01 — The Mouse Who Wanted to Fly

Z02 — A Very Strange Breakfast

Z03 — The Giant's Little Shoes

Z04 — The Door Under the Tree

Z05 — The Birthday Nobody Remembered

**Sửu — Buffalo**

Z06 — The Buffalo and the Golden Seed

Z07 — A House for the Rain

Z08 — The Bridge That Was Too Short

Z09 — The Market on the Moon

Z10 — The Day the River Stopped

**Dần — Tiger**

Z11 — The Tiger Afraid of the Dark

Z12 — The Mountain That Moved

Z13 — The Whispering Waterfall

Z14 — A Doctor for the Forest

Z15 — The Smallest Hero

**Mão — Cat**

Z16 — The Cat and the Magic Mirror

Z17 — The Shop That Sold Dreams

Z18 — The Missing Red Lantern

Z19 — A Room Without a Floor

Z20 — The Cat Who Could Hear Colors

**Thìn — Dragon**

Z21 — The Dragon Without Fire

Z22 — The Cloud Collector

Z23 — The Lake of a Thousand Stars

Z24 — The Floating Village

Z25 — The Dragon's Greatest Gift

**Tỵ — Snake**

Z26 — The Snake and the Empty Book

Z27 — The Puzzle of Seven Stones

Z28 — The Tree That Forgot Its Name

Z29 — The Invisible Teacher

Z30 — The Question Without an Answer

**Ngọ — Horse**

Z31 — The Horse Who Missed the Train

Z32 — The Road to Nowhere

Z33 — The Hotel in the Clouds

Z34 — A Ticket to Yesterday

Z35 — The Journey Home

**Mùi — Goat**

Z36 — The Goat Who Painted the Wind

Z37 — The Silent Music Festival

Z38 — The Broken Rainbow

Z39 — A Song for the Moon

Z40 — The Beautiful Mistake

**Thân — Monkey**

Z41 — The Monkey's Talking Machine

Z42 — The Robot Who Wanted a Holiday

Z43 — The Upside-Down City

Z44 — The Button Nobody Should Press

Z45 — The Invention That Saved Nothing

**Dậu — Rooster**

Z46 — The Rooster Who Overslept

Z47 — The Clock That Ran Backward

Z48 — A Day Without Morning

Z49 — The Great Sleep Contest

Z50 — The Perfect Schedule

**Tuất — Dog**

Z51 — The Dog and the Lost Star

Z52 — The Village Without Smiles

Z53 — The Promise Under the Banyan Tree

Z54 — The Stranger at the Gate

Z55 — The Longest Night

**Hợi — Pig**

Z56 — The Pig and the Endless Bowl

Z57 — The Secret Recipe

Z58 — The Festival of a Thousand Lights

Z59 — The Feast for Everyone

Z60 — The Greatest Treasure

## 8. YÊU CẦU KỸ THUẬT

Hãy nghiên cứu kiến trúc hiện tại của Deep Flow Language trước khi viết mã.

Thiết kế hệ thống có các thành phần:

**Curriculum Manager**

Quản lý 72 bài học, nội dung, phiên bản và trạng thái xuất bản.

**Vocabulary Engine**

Quản lý từ vựng, phân bổ từ, kiểm tra độ bao phủ và lịch ôn tập.

**Story Script Manager**

Lưu trữ kịch bản, lời thoại, câu hỏi, đáp án và nội dung từng audio.

**Audio Production**

Hỗ trợ tạo audio bằng dịch vụ TTS thông qua API, quản lý giọng đọc, tốc độ, khoảng dừng và thời lượng.

**Interactive Listening**

Phát audio theo từng phần, hiển thị phụ đề tùy chọn, tạm dừng để người học trả lời.

**Learning Progress**

Theo dõi tiến độ, kết quả luyện tập và đề xuất nội dung ôn tập.

## 9. QUẢN LÝ API VÀ CHI PHÍ

Ứng dụng đang sử dụng hoặc có kế hoạch sử dụng các dịch vụ AI như OpenAI và Gemini.

Yêu cầu:

- Không đưa API key vào mã nguồn.
- Không ghi API key vào log.
- Quản lý giới hạn sử dụng và chi phí.
- Có cơ chế retry và exponential backoff.
- Cache kết quả phù hợp.
- Tránh tạo lại audio không cần thiết.
- Có thể thay đổi nhà cung cấp AI/TTS.
- Không gọi API trả phí khi chưa được phê duyệt.

## 10. PHÂN CÔNG CHATGPT VÀ CODEX

**ChatGPT**

- Phát triển cốt truyện.
- Viết kịch bản tiếng Anh.
- Thiết kế câu hỏi phản xạ.
- Phân tích từ vựng.
- Đánh giá tính hấp dẫn và dễ hiểu.

**Codex**

- Thiết kế kiến trúc phần mềm.
- Xây dựng cấu trúc dữ liệu.
- Phát triển Vocabulary Engine.
- Xây dựng công cụ kiểm thử.
- Tích hợp audio và giao diện học tập.
- Tối ưu hiệu suất và chi phí API.

## 11. NHIỆM VỤ ĐẦU TIÊN CỦA CODEX

Hãy thực hiện theo thứ tự:

1. Kiểm tra repository hiện tại.
2. Xác định công nghệ, cấu trúc thư mục và chức năng đang có.
3. Kiểm tra xem ứng dụng đã có hệ thống bài học, từ vựng và audio hay chưa.
4. Đề xuất kiến trúc tích hợp giáo trình mới, ưu tiên tái sử dụng thành phần hiện có.
5. Tạo cấu trúc dữ liệu chuẩn cho 72 bài học.
6. Xây dựng phiên bản đầu tiên của Vocabulary Engine.
7. Viết kiểm thử tự động cho các quy tắc dữ liệu.
8. Báo cáo kết quả và các bước tiếp theo.

**Các giới hạn bắt buộc:**

- Không xóa hoặc làm hỏng chức năng hiện có.
- Không tự ý thay đổi kiến trúc lớn.
- Không tự ý triển khai production.
- Không gọi API trả phí.
- Không tự sáng tác thay thế nội dung giáo trình đã được ChatGPT phê duyệt.
- Không tuyên bố hoàn thành khi chưa kiểm thử.
- Chỉ commit hoặc push thay đổi sau khi người dùng phê duyệt.

Hãy bắt đầu bằng việc kiểm tra mã nguồn và trình bày kế hoạch triển khai cụ thể.