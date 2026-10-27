#!/usr/bin/env python3
"""Nâng số phiên bản + thêm ghi chú cập nhật.
Quy tắc (xem HUONG_DAN_LAP_TRINH.md): beta chạy 1 → 99; sau beta 99 tăng số cuối phiên bản và quay về beta 1
(1.0.3-beta.99 → 1.0.4-beta.1); bản vá vượt 99 thì lên bản phụ (1.0.99 → 1.1.0-beta.1).
Dùng:  python3 tools/bump.py "mục 1" "mục 2" ...        (bản web)
       python3 tools/bump.py --apk "mục 1" ...          (kèm nâng APK: versionCode +1, versionName theo cùng quy tắc)
"""
import json, re, sys, datetime, pathlib
R = pathlib.Path(__file__).resolve().parent.parent
def nxt(v):
    m = re.match(r'^(\d+)\.(\d+)\.(\d+)(?:-beta\.(\d+))?$', v)
    a, b, c, n = int(m[1]), int(m[2]), int(m[3]), int(m[4] or 0)
    if m[4] is None or n >= 99: c, n = c + 1, 0                                          # sau bản chính thức → chu kỳ beta mới
    n += 1
    if c > 99: b, c = b + 1, 0
    return f'{a}.{b}.{c}', f'{a}.{b}.{c}-beta.{n}'
args = sys.argv[1:]; apk = '--apk' in args; items = [x for x in args if x != '--apk']
vf = R / 'update/version.json'; v = json.loads(vf.read_text())
old = v['ver']; web, new = nxt(old)
d = datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=7))).strftime('%d/%m/%Y')
e = {'v': new, 'd': d, 'items': items}
v['ver'] = new; v['web'] = web; v['changelog'].insert(0, e)
if apk:
    v['apk']['versionCode'] += 1; v['apk']['versionName'] = nxt(v['apk']['versionName'])[1]
vf.write_text(json.dumps(v, ensure_ascii=False, indent=2))
p = R / 'www/index.html'; h = p.read_text()
h = h.replace(f"const APP_VER='{old}';", f"const APP_VER='{new}';")
h = h.replace('const CHANGELOG=[', 'const CHANGELOG=[' + json.dumps(e, ensure_ascii=False) + ', ', 1)
p.write_text(h)
print(old, '→', new, ('· APK ' + v['apk']['versionName']) if apk else '')
