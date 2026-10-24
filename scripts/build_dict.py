"""Đóng gói từ điển offline cho Deep Flow.
- Anh–Việt: Free Vietnamese Dictionary Project (Hồ Ngọc Đức, GPL)
- Anh–Anh: Princeton WordNet 3.0 (giấy phép WordNet, cho phép dùng & phát hành lại)
Chỉ giữ khoảng 40.000 từ thông dụng nhất (wordfreq) + dạng gốc của chúng.
Kết quả: en-vi.json.gz, en-en.json.gz
"""
import gzip, json, re, sys, os
from wordfreq import top_n_list

DL = sys.argv[1]
OUT = sys.argv[2]
N = int(sys.argv[3]) if len(sys.argv) > 3 else 40000
os.environ['NLTK_DATA'] = os.path.join(DL, 'nltk_data')
import nltk
nltk.data.path.insert(0, os.path.join(DL, 'nltk_data'))
from nltk.corpus import wordnet as wn

freq = [w for w in top_n_list('en', N * 2) if re.fullmatch(r"[a-z][a-z'\-]*", w)]

# ---------- Anh–Việt (FVDP) ----------
fv = {}
cur = None
def ipa_fix(x):
    x = x.strip().strip('/').strip()
    return x.replace("'", "ˈ").replace(",", "ˌ").replace(':', 'ː')
for line in open(os.path.join(DL, 'VietAnh/anhviet.dict'), encoding='utf-8-sig'):
    line = line.rstrip('\n')
    if not line:
        continue
    c = line[0]
    if c == '@':
        m = re.match(r'@(.*?)(?:\s+/(.*)/)?\s*$', line)
        head = (m.group(1) if m else '').strip()
        if not head or head.startswith('00-') or head.lower() in fv:
            cur = None  # bỏ dòng thông tin / mục trùng (giữ mục đầu)
            continue
        cur = {'ipa': ipa_fix(m.group(2) or ''), 'pos': [], 'idm': []}
        fv[head.lower()] = cur
        continue
    if cur is None:
        continue
    if c == '*':
        cur['pos'].append([line[1:].strip(), []]); cur['_last'] = 'pos'
    elif c == '-':
        mean = line[1:].strip()
        if cur.get('_last') == 'idm' and cur['idm']:
            if len(cur['idm'][-1][1]) < 2: cur['idm'][-1][1].append(mean)
            continue
        if not cur['pos']:
            cur['pos'].append(['', []])
        senses = cur['pos'][-1][1]
        senses.append([mean, []])
    elif c == '=':
        body = line[1:]
        en, _, vi = body.partition('+')
        tgt = None
        if cur.get('_last') == 'idm' and cur['idm']:
            continue
        if cur['pos'] and cur['pos'][-1][1]:
            tgt = cur['pos'][-1][1][-1][1]
        if tgt is not None and len(tgt) < 2:
            tgt.append([en.strip(), vi.strip()])
    elif c == '!':
        cur['idm'].append([line[1:].strip(), []]); cur['_last'] = 'idm'

def pack_vi(e):
    pos = []
    for p, senses in e['pos']:
        ss = [[m] + ([ex] if ex else []) for m, ex in senses[:6]]
        if ss: pos.append([p, ss])
    idm = [[i, ' / '.join(ms)] for i, ms in e['idm'][:5] if ms]
    return [e['ipa'], pos] + ([idm] if idm else [])

# ---------- chọn từ ----------
keep = []
seen = set()
for w in freq:
    cands = [w]
    try:
        for p in ('n', 'v', 'a', 'r'):
            b = wn.morphy(w, p)
            if b and b not in cands: cands.append(b)
    except Exception:
        pass
    for x in cands:
        if x in seen: continue
        if x in fv or wn.synsets(x):
            seen.add(x); keep.append(x)
    if len(keep) >= N:
        break

envi = {w: pack_vi(fv[w]) for w in keep if w in fv and fv[w]['pos']}
# cụm từ nhiều chữ (give up, look after, in spite of…): mọi chữ trong cụm đều thuộc bộ từ thông dụng
common = set(keep) | set(freq[:N])
nph = 0
for h, e in fv.items():
    ws = h.split()
    if 2 <= len(ws) <= 4 and all(re.fullmatch(r"[a-z][a-z'\-]*", x) for x in ws) and all(x in common for x in ws) and any(ss for _, ss in e['pos']):
        envi[h] = pack_vi(e); nph += 1
# cụm động từ / thành ngữ nằm trong mục của từ chính: "!to give up" → mục "give up"
for h, e in fv.items():
    if h not in common: continue
    for idm, ms in e['idm']:
        if not ms: continue
        k = re.sub(r'^to\s+', '', idm.strip().lower())
        k = re.sub(r'\s*[;,].*$', '', k).strip()
        ws = k.split()
        if not (2 <= len(ws) <= 4) or k in envi: continue
        if not all(re.fullmatch(r"[a-z][a-z'\-]*", x) for x in ws): continue
        if re.search(r"\b(someone|something|somebody|sb|sth|oneself|one's)\b", k): continue
        envi[k] = ['', [['cụm từ', [[m] for m in ms[:3]]]]]; nph += 1
print('cụm từ', nph)
EN_N = int(os.environ.get('EN_N', '40000'))
en_keep = set(k for k in envi if ' ' not in k) | set(keep[:EN_N])

# ---------- Anh–Anh (WordNet) ----------
PN = {'n': 'noun', 'v': 'verb', 'a': 'adjective', 's': 'adjective', 'r': 'adverb'}
enen = {}
for w in keep:
    if w not in en_keep: continue
    ss = wn.synsets(w)
    if not ss: continue
    by = {}
    for s in ss:
        p = PN[s.pos()]
        lst = by.setdefault(p, [])
        if len(lst) >= 4: continue
        syn = [l.name().replace('_', ' ') for l in s.lemmas() if l.name().lower() != w][:4]
        ex = s.examples()[:1]
        item = [s.definition()]
        if ex or syn: item.append(ex[0] if ex else '')
        if syn: item.append(syn)
        lst.append(item)
    enen[w] = [[p, v] for p, v in by.items()]

def dump(obj, name):
    data = json.dumps(obj, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
    with gzip.open(os.path.join(OUT, name), 'wb', compresslevel=9) as f:
        f.write(data)
    print(name, len(obj), 'từ', round(len(data) / 1e6, 2), 'MB →', round(os.path.getsize(os.path.join(OUT, name)) / 1e6, 2), 'MB nén')

os.makedirs(OUT, exist_ok=True)
dump(envi, 'en-vi.json.gz')
dump(enen, 'en-en.json.gz')
print('approach:', json.dumps(envi.get('approach'), ensure_ascii=False)[:600])
print('approach EN:', json.dumps(enen.get('approach'), ensure_ascii=False)[:500])
