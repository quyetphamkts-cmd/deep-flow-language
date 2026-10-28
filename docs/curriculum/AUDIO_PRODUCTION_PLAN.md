# Phương án audio tự nhiên cho giáo trình — chưa gọi API

Đối chiếu tài liệu nhà cung cấp ngày 28/10/2026. Đây là đề xuất sản xuất, không phải kết quả nghe thử hoặc khẳng định nhà cung cấp nào hay nhất. Chưa phát sinh chi phí, chưa tạo audio thật.

## Lựa chọn

- Giọng người thật: thuê người đọc/diễn viên bản ngữ và biên tập thu âm là hướng phù hợp nếu ưu tiên diễn xuất tự nhiên cao nhất và có ngân sách. Thử đoạn có lời kể và đối thoại trước khi thu toàn bộ.
- AI: ưu tiên đưa ElevenLabs Text to Dialogue vào vòng nghe thử vì có đối thoại nhiều giọng và điều khiển biểu cảm. Gemini TTS là lựa chọn đối chiếu có điều khiển phong cách/nhịp và nhiều người nói. OpenAI Speech là lựa chọn đối chiếu cho giọng kể có chỉ dẫn cách đọc. Không kết luận chất lượng khi chưa nghe cùng một mẫu.
- Dùng giọng được cấp phép; chỉ dùng giọng sao chép khi có sự đồng ý của người sở hữu. Ghi rõ audio AI nếu chọn tổng hợp. Không suy ra quyền dùng thương mại từ việc API có thể truy cập; kiểm tra gói/điều khoản trước khi mua.

## Mẫu cần duyệt trước khi sản xuất hàng loạt

1. Chốt một đoạn 60–90 giây từ C01 có người kể, David và Anna; thêm một câu hỏi/đáp án. Lập hồ sơ giọng ổn định cho từng nhân vật, giọng Anh-Mỹ hoặc Anh-Anh nhất quán, cách đọc tên riêng và những từ dễ nhầm.
2. Đọc rõ như kể chuyện tự nhiên, ngắt theo ý, giữ nối âm và nhấn câu; không cố kéo dài từng từ. So sánh nhịp theo mức người học, không coi một tốc độ cố định là tự nhiên cho mọi đoạn.
3. Tạo theo đoạn/cảnh đủ ngữ cảnh để giữ cảm xúc. Tránh ghép mỗi từ hoặc mỗi câu rời khi không cần; giữ cùng cấu hình giọng giữa các khối, thử lại riêng khối bị lỗi. Mọi lời phải khớp bản thảo đã chốt, không cho AI tự thêm đối thoại vào bản thu.
4. Với Quick Response: thu câu hỏi và đáp án riêng; chèn **3.000 ms im lặng bằng phần mềm** sau câu hỏi, rồi mới ghép đáp án. Không phó mặc độ dài khoảng chờ cho câu lệnh TTS. Người học có thể trả lời một/vài từ.
5. Nghe duyệt phát âm, vai nhân vật, cảm xúc, nhịp, chỗ nối và âm lượng. Đối chiếu transcript để phát hiện thêm/bớt/nhầm từ; nhận dạng giọng nói chỉ hỗ trợ, vẫn phải nghe duyệt. Chốt người/giọng rồi mới tạo toàn bộ bài.
6. Giữ bản WAV gốc để biên tập; xuất định dạng app hỗ trợ, đồng nhất âm lượng và tránh clipping. Căn thời gian lại trên **audio sau cùng**, cộng cả các khoảng nghỉ; không dùng thời gian ước lượng theo số chữ làm phụ đề chính thức. Timestamp của nhà cung cấp là đầu vào để kiểm tra, không tự chứng minh mọi từ đúng.
7. Lưu nội dung/phiên bản/giọng/model/cấu hình trong khóa cache, hash của audio, lịch sử duyệt và thời lượng đo thật; chỉ xuất bản phần đã duyệt. Không tạo lại toàn bộ mỗi lần mở app, không để phát sinh phí ở thiết bị người học.

## Trạng thái hiện tại và giới hạn

Danh sách chuẩn và phân bổ đã có nhưng bản thảo chưa bao phủ đủ mục tiêu từ. Vì vậy chỉ nên làm pilot để chọn giọng; chưa nên chi tiền thu hàng loạt 12 bài rồi phải thu lại. Cần duyệt nhà cung cấp/giọng và mức chi phí trước khi gọi API trả phí. Không ghi API key vào repo, cấu hình nội dung hoặc log.

## Nguồn chính thức

- ElevenLabs Text to Dialogue: https://elevenlabs.io/docs/overview/capabilities/text-to-dialogue
- ElevenLabs dialogue + timestamps / pronunciation dictionaries: https://elevenlabs.io/docs/api-reference/text-to-dialogue/convert-with-timestamps
- Gemini TTS: https://ai.google.dev/gemini-api/docs/speech-generation
- OpenAI Text to Speech: https://developers.openai.com/api/docs/guides/text-to-speech

Tên model, giá, quyền sử dụng và giới hạn có thể đổi; cần kiểm tra lại ở lúc thực hiện. Chưa chọn cứng một model hoặc hứa audio AI giống người thật tuyệt đối.
