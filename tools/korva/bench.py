# đo nhanh bản int8 (trọng số int8 + DequantizeLinear): có / không gộp sẵn trọng số khi mở mô hình
import time, resource, sys, numpy as np, onnxruntime as ort, korvatts.session as ks
from pathlib import Path
from korvatts import TTS
d = Path("kv")
(d / "onnx").mkdir(exist_ok=True); (d / "voice_styles").mkdir(exist_ok=True)
import json, shutil
for n in ["duration_predictor", "text_encoder", "vector_estimator", "vocoder"]: shutil.copy(d / f"{n}.int8.onnx", d / "onnx" / f"{n}.onnx")
for n in ["tts.json", "unicode_indexer.json"]: shutil.copy(d / n, d / "onnx" / n)
for k, v in json.load(open(d / "voices.json")).items(): json.dump(v, open(d / "voice_styles" / f"{k}.json", "w"))
fold = sys.argv[1] == "1"
orig = ort.SessionOptions
class SO(orig):
    def __init__(s):
        super().__init__()
        if fold: s.add_session_config_entry("session.disable_quant_qdq", "1")
ks.ort.SessionOptions = SO
t0 = time.time(); t = TTS(assets_dir=d, device="cpu", num_threads=2); print("fold", fold, "load", round(time.time() - t0, 2))
t.synthesize("khởi động", voice="khanh_vy", total_steps=2, seed=1)
for steps in (6, 10, 16):
    t0 = time.time(); w, dur = t.synthesize("Đó là lúc năm giờ và Allen đang đi mô tô ở San Francisco.", voice="khanh_vy", total_steps=steps, seed=7)
    print(f"BENCH fold={fold} steps={steps}: {time.time()-t0:.2f}s for {dur:.2f}s")
print("peak RSS MB", resource.getrusage(resource.RUSAGE_SELF).ru_maxrss // 1024)
