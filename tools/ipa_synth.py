# Tạo file âm thanh cho từng âm IPA (đọc riêng âm đó) bằng Piper TTS (giọng Anh – Anh), chạy trên GitHub Actions
import json, os, subprocess, sys, wave
from piper import PiperVoice, SynthesisConfig
VOICE = sys.argv[1]; OUT = sys.argv[2]; os.makedirs(OUT, exist_ok=True)
v = PiperVoice.load(VOICE + '.onnx')
idmap = v.config.phoneme_id_map
# tên file · chuỗi âm (theo ký hiệu espeak) · kiểu: v = nguyên âm, c = phụ âm kéo dài được, p = phụ âm bật
P = [('i_long','ˈiː','v'),('i_short','ˈɪ','v'),('e','ˈe','v'),('ae','ˈæ','v'),('uh','ˈʌ','v'),('a_long','ˈɑː','v'),('o_short','ˈɒ','v'),('o_long','ˈɔː','v'),
     ('u_short','ˈʊ','v'),('u_long','ˈuː','v'),('schwa','ə','v'),('er_long','ˈɜː','v'),
     ('ei','ˈeɪ','v'),('ai','ˈaɪ','v'),('oi','ˈɔɪ','v'),('au','ˈaʊ','v'),('ou','ˈəʊ','v'),('ia','ˈɪə','v'),('ea','ˈeə','v'),('ua','ˈʊə','v'),
     ('p','p','p'),('b','b','p'),('t','t','p'),('d','d','p'),('k','k','p'),('g','ɡ','p'),('ch','tʃ','p'),('j','dʒ','p'),
     ('f','f','c'),('v','v','c'),('th','θ','c'),('dh','ð','c'),('s','s','c'),('z','z','c'),('sh','ʃ','c'),('zh','ʒ','c'),
     ('m','m','c'),('n','n','c'),('ng','ŋ','c'),('h','h','c'),('l','l','c'),('r','ɹ','c'),('w','w','c'),('y','j','c')]
rep = {}
for name, ph, kind in P:
    chars = list(ph)
    miss = [c for c in chars if c not in idmap]
    if miss: rep[name] = 'missing ' + ''.join(miss); continue
    ls = {'v': 1.35, 'c': 2.2, 'p': 1.0}[kind]
    variants = {'': chars}
    if kind == 'c': variants[''] = chars + chars  # kéo dài phụ âm (ssss, mmmm)
    for suf, cs in variants.items():
        ids = v.phonemes_to_ids(cs)
        cfg = SynthesisConfig(length_scale=ls, noise_scale=0.4, noise_w_scale=0.5)
        audio = v.phoneme_ids_to_audio(ids, cfg)
        import numpy as np
        a = np.asarray(audio, dtype=np.float32)
        wav = f'{OUT}/{name}{suf}.wav'
        with wave.open(wav, 'wb') as w:
            w.setnchannels(1); w.setsampwidth(2); w.setframerate(v.config.sample_rate)
            w.writeframes((np.clip(a, -1, 1) * 32767).astype('<i2').tobytes())
        mp3 = f'{OUT}/{name}{suf}.mp3'
        subprocess.run(['ffmpeg','-y','-loglevel','error','-i',wav,'-af','silenceremove=start_periods=1:start_threshold=-45dB:stop_periods=-1:stop_threshold=-45dB:stop_duration=0.15,apad=pad_dur=0.08,loudnorm=I=-18:TP=-2','-ar','24000','-ac','1','-b:a','48k',mp3], check=True)
        os.remove(wav)
        rep[name+suf] = round(len(a) / v.config.sample_rate, 2)
json.dump(rep, open(f'{OUT}/report.json','w'), ensure_ascii=False, indent=1)
print(rep)
