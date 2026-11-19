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
import onnx.version_converter

for name in ["duration_predictor", "text_encoder", "vector_estimator", "vocoder"]:
    m = onnx.load(str(src / "onnx" / f"{name}.onnx"), load_external_data=False)
    ops = {}
    for n in m.graph.node: ops[n.op_type] = ops.get(n.op_type, 0) + 1
    print(name, "inputs:", [(i.name, [d.dim_value or d.dim_param for d in i.type.tensor_type.shape.dim]) for i in m.graph.input],
          "outputs:", [o.name for o in m.graph.output], "ops:", dict(sorted(ops.items(), key=lambda x: -x[1])[:12]))

# ---- int8 "chỉ trọng số": trọng số Conv/MatMul/Gemm lưu int8 (theo từng kênh) + DequantizeLinear → file nhỏ ~4 lần,
#      phép tính vẫn bằng float nên chất lượng gần như giữ nguyên ----
from onnx import numpy_helper, helper, TensorProto
def wq(srcf, dst):
    m = onnx.load(str(srcf))
    op = max([o.version for o in m.opset_import if o.domain in ("", "ai.onnx")] or [0])
    if op < 13:
        m = onnx.version_converter.convert_version(m, 13); op = 13
    inits = {i.name: i for i in m.graph.initializer}
    uses = {}
    for n in m.graph.node:
        for k, x in enumerate(n.input): uses.setdefault(x, []).append((n.op_type, k))
    new_nodes, done, saved = [], 0, 0
    for name, init in list(inits.items()):
        u = uses.get(name, [])
        if not u or not all((t in ("Conv", "ConvTranspose") and k == 1) or (t in ("MatMul", "Gemm") and k == 1) for t, k in u): continue
        w = numpy_helper.to_array(init)
        if w.dtype != np.float32 or w.size < 4096: continue
        t0 = u[0][0]
        axis = 0 if t0 == "Conv" else (1 if t0 == "ConvTranspose" else w.ndim - 1)
        red = tuple(i for i in range(w.ndim) if i != axis)
        mx = np.max(np.abs(w), axis=red)
        sc = np.where(mx > 0, mx / 127.0, 1.0).astype(np.float32)
        shp = [1] * w.ndim; shp[axis] = -1
        qw = np.clip(np.round(w / sc.reshape(shp)), -127, 127).astype(np.int8)
        m.graph.initializer.remove(init)
        m.graph.initializer.extend([numpy_helper.from_array(qw, name + "_q"), numpy_helper.from_array(sc, name + "_s"), numpy_helper.from_array(np.zeros_like(sc, dtype=np.int8), name + "_z")])
        new_nodes.append(helper.make_node("DequantizeLinear", [name + "_q", name + "_s", name + "_z"], [name], name=name + "_dq", axis=axis))
        done += 1; saved += w.nbytes - qw.nbytes
    for nd in reversed(new_nodes): m.graph.node.insert(0, nd)
    onnx.save(m, str(dst))
    print(f"int8w {srcf.name}: {done} weights, opset {op}, {srcf.stat().st_size/1e6:.1f} -> {dst.stat().st_size/1e6:.1f} MB")

q = Path("q"); (q / "onnx").mkdir(parents=True, exist_ok=True)
shutil.copytree(src / "voice_styles", q / "voice_styles", dirs_exist_ok=True)
for f in (src / "onnx").iterdir():
    if f.suffix == ".json": shutil.copy(f, q / "onnx" / f.name)
for name in ["duration_predictor", "text_encoder", "vector_estimator", "vocoder"]:
    srcf = src / "onnx" / f"{name}.onnx"; dst = q / "onnx" / f"{name}.onnx"
    try:
        wq(srcf, dst)
        ort.InferenceSession(str(dst), providers=["CPUExecutionProvider"])
    except Exception as e:
        print("int8w FAILED", name, repr(e)[:400]); shutil.copy(srcf, dst)

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

# ---- gói cho app: bản int8 (nhẹ) cho cả 4 phần + bản gốc của vector_estimator (chất lượng cao, tuỳ chọn) ----
for name in ["duration_predictor", "text_encoder", "vector_estimator", "vocoder"]:
    shutil.copy(q / "onnx" / f"{name}.onnx", OUT / f"{name}.int8.onnx")
    shutil.copy(src / "onnx" / f"{name}.onnx", OUT / f"{name}.onnx")
for f in ["tts.json", "unicode_indexer.json"]: shutil.copy(src / "onnx" / f, OUT / f)
allv = {}
for v in voices:
    allv[v] = json.loads((src / "voice_styles" / f"{v}.json").read_text("utf-8"))
(OUT / "voices.json").write_text(json.dumps(allv, separators=(",", ":")), "utf-8")
for f in src.iterdir():
    if f.is_file() and (f.name.startswith(("LICENSE", "NOTICE", "MODEL_LICENSE"))): shutil.copy(f, OUT / f"KorvaTTS-{f.name}")
for f in sorted(OUT.iterdir()): print("OUT", f.name, f"{f.stat().st_size/1e6:.1f} MB")
