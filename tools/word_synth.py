# Đọc từng từ / câu (mỗi dòng 1 mục) thành file mp3 bằng Piper TTS — chạy trên GitHub Actions (dùng cho trò Chùa Từ Vựng)
import os, re, subprocess, sys, wave
from piper import PiperVoice, SynthesisConfig
VOICE, LIST, OUT = sys.argv[1], sys.argv[2], sys.argv[3]; os.makedirs(OUT, exist_ok=True)
v = PiperVoice.load(VOICE + '.onnx')
cfg = SynthesisConfig(length_scale=1.12, noise_scale=0.5, noise_w_scale=0.6)
for line in open(LIST, encoding='utf-8'):
    t = line.strip()
    if not t: continue
    name = re.sub(r'[^a-z0-9]+', '_', t.lower()).strip('_')
    wav = f'{OUT}/{name}.wav'
    with wave.open(wav, 'wb') as w: v.synthesize_wav(t + '.', w, syn_config=cfg)
    subprocess.run(['ffmpeg','-y','-loglevel','error','-i',wav,'-af','silenceremove=start_periods=1:start_threshold=-45dB:stop_periods=-1:stop_threshold=-45dB:stop_duration=0.2,apad=pad_dur=0.1,loudnorm=I=-16:TP=-1.5','-ar','24000','-ac','1','-b:a','40k',f'{OUT}/{name}.mp3'], check=True)
    os.remove(wav); print(name)
