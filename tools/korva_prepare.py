"""Chuẩn bị giọng KorvaTTS cho app (chạy trên GitHub Actions — máy có mạng tới Hugging Face).

1. Tải dogenthq/KorvaTTS (Apache-2.0): onnx/*, voice_styles/*, LICENSE/NOTICE.
2. Lượng tử hoá int8 (MatMul/Gemm) để file nhẹ và chạy nhanh hơn trên điện thoại.
3. Đo tốc độ + so chất lượng int8 với bản gốc (cùng hạt giống ngẫu nhiên), xuất mẫu giọng.
4. Gom 10 giọng vào voices.json; mọi file cần cho app nằm trong out/ để đưa lên GitHub Release.
"""
import json, os, shutil, sys, time
from pathlib import Path

import numpy as np
import soundfile as sf
from huggingface_hub import snapshot_download

OUT = Path("out"); OUT.mkdir(exist_ok=True)
SAMP = Path("samples"); SAMP.mkdir(exist_ok=True)
src = Path(snapshot_download("dogenthq/KorvaTTS", allow_patterns=["onnx/*", "voice_styles/*", "LICENSE*", "NOTICE*", "MODEL_LICENSE*", "README.md"]))
print("downloaded:", src)
for p in sorted(src.rglob("*")):
    if p.is_file():
        print(f"  {p.relative_to(src)}  {p.stat().st_size/1e6:.1f} MB")

import onnx
import onnxruntime as ort
from onnxruntime.quantization import quantize_dynamic, QuantType

for name in ["duration_predictor", "text_encoder", "vector_estimator", "vocoder"]:
    m = onnx.load(str(src / "onnx" / f"{name}.onnx"), load_external_data=False)
    ops = {}
    for n in m.graph.node: ops[n.op_type] = ops.get(n.op_type, 0) + 1
    print(name, "inputs:", [(i.name, [d.dim_value or d.dim_param for d in i.type.tensor_type.shape.dim]) for i in m.graph.input],
          "outputs:", [o.name for o in m.graph.output], "ops:", dict(sorted(ops.items(), key=lambda x: -x[1])[:12]))

# ---- int8 ----
q = Path("q"); (q / "onnx").mkdir(parents=True, exist_ok=True)
shutil.copytree(src / "voice_styles", q / "voice_styles", dirs_exist_ok=True)
for f in (src / "onnx").iterdir():
    if f.suffix == ".json": shutil.copy(f, q / "onnx" / f.name)
for name in ["duration_predictor", "text_encoder", "vector_estimator", "vocoder"]:
    srcf = src / "onnx" / f"{name}.onnx"; dst = q / "onnx" / f"{name}.onnx"
    try:
        quantize_dynamic(str(srcf), str(dst), weight_type=QuantType.QInt8, op_types_to_quantize=["MatMul", "Gemm"], per_channel=True)
        print("int8", name, f"{srcf.stat().st_size/1e6:.1f} → {dst.stat().st_size/1e6:.1f} MB")
    except Exception as e:
        print("int8 FAILED", name, e); shutil.copy(srcf, dst)

from korvatts import TTS
texts = ["Xin chào, hôm nay chúng ta học bài mới.", "Đó là lúc năm giờ và Allen đang đi mô tô ở San Francisco."]
res = {}
for tag, d in [("fp32", src), ("int8", q)]:
    for thr in (2, 4):
        t = TTS(assets_dir=d, device="cpu", num_threads=thr)
        t.synthesize("khởi động", voice="khanh_vy", total_steps=2, seed=1)
        for steps in (8, 16, 32):
            t0 = time.time(); wav, dur = t.synthesize(texts[1], voice="khanh_vy", total_steps=steps, seed=7); el = time.time() - t0
            res[(tag, thr, steps)] = wav
            print(f"BENCH {tag} threads={thr} steps={steps}: {el:.2f}s for {dur:.2f}s audio (RTF {el/dur:.2f})")
            if thr == 4: sf.write(SAMP / f"cmp_{tag}_{steps}buoc.wav", wav, t.sample_rate)
for steps in (8, 16, 32):
    a, b = res[("fp32", 4, steps)], res[("int8", 4, steps)]
    n = min(len(a), len(b)); err = a[:n] - b[:n]
    snr = 10 * np.log10(np.sum(a[:n] ** 2) / max(1e-12, np.sum(err ** 2)))
    print(f"QUALITY steps={steps}: int8 vs fp32 SNR {snr:.1f} dB, len {len(a)} vs {len(b)}")

# ---- mẫu từng giọng (int8, 16 bước) ----
t = TTS(assets_dir=q, device="cpu", num_threads=4)
voices = t.list_voices(); print("voices:", voices)
for v in voices:
    wav, _ = t.synthesize("Xin chào! Mình là giọng đọc tiếng Việt. Hết mỗi câu tiếng Anh, mình sẽ đọc bản dịch cho bạn nghe.", voice=v, total_steps=16, seed=3)
    sf.write(SAMP / f"{v}.wav", wav, t.sample_rate)

# ---- gói cho app ----
for name in ["duration_predictor", "text_encoder", "vocoder"]:
    a, b = src / "onnx" / f"{name}.onnx", q / "onnx" / f"{name}.onnx"
    shutil.copy(a, OUT / f"{name}.onnx")            # phần nhỏ: giữ bản gốc cho chắc chất lượng
    shutil.copy(b, OUT / f"{name}.int8.onnx")
shutil.copy(src / "onnx" / "vector_estimator.onnx", OUT / "vector_estimator.onnx")
shutil.copy(q / "onnx" / "vector_estimator.onnx", OUT / "vector_estimator.int8.onnx")
for f in ["tts.json", "unicode_indexer.json"]: shutil.copy(src / "onnx" / f, OUT / f)
allv = {}
for v in voices:
    allv[v] = json.loads((src / "voice_styles" / f"{v}.json").read_text("utf-8"))
(OUT / "voices.json").write_text(json.dumps(allv, separators=(",", ":")), "utf-8")
for f in src.iterdir():
    if f.is_file() and (f.name.startswith(("LICENSE", "NOTICE", "MODEL_LICENSE"))): shutil.copy(f, OUT / f"KorvaTTS-{f.name}")
for f in sorted(OUT.iterdir()): print("OUT", f.name, f"{f.stat().st_size/1e6:.1f} MB")
