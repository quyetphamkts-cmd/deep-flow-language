'use strict';
const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const {reviewDraft}=require('./draft-review.cjs');
function renderPreview(manifest,bundle,inventory,characters){
  const report=reviewDraft(manifest,bundle,inventory);
  if(characters?.family?.members?.length!==5 || characters.family.size!==5)throw new Error('Thiếu thông tin gia đình David đã xác nhận');
  const payload=JSON.stringify({manifest,bundle,inventory,characters,report:{lessons:report.lessons}}).replace(/</g,'\\u003c').replace(/\u2028/g,'\\u2028').replace(/\u2029/g,'\\u2029');
  const js=fs.readFileSync(path.join(__dirname,'review-preview.js'),'utf8'),hash=crypto.createHash('sha256').update(js).digest('base64');
  return `<!doctype html>
<html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'sha256-${hash}'; style-src 'unsafe-inline'; connect-src 'none'; base-uri 'none'; form-action 'none'">
<title>Deep Flow · Đọc thử giáo trình</title>
<style>
:root{color-scheme:light;--ink:#173d3b;--muted:#566c68;--paper:#fffdf8;--line:#dbe4db;--accent:#176b58}*{box-sizing:border-box}body{margin:0;background:#edf2e9;color:var(--ink);font:16px/1.6 system-ui,sans-serif}main{max-width:900px;margin:auto;padding:28px 20px 48px}header{display:flex;gap:14px;align-items:center;margin-bottom:24px}.logo{border-radius:50%;background:var(--ink);color:#f5efcf;padding:13px;font-weight:800}h1{font-size:23px;margin:0}header p{margin:0;color:var(--muted)}.badge{display:inline-block;background:#f6e6b7;color:#5d4918;padding:4px 12px;border-radius:20px;font-size:13px}.lessons{display:flex;gap:10px;margin:18px 0;flex-wrap:wrap}.lessons button{flex:1;min-width:80px}.lessons [aria-pressed=true]{background:var(--ink);color:white;border-color:var(--ink)}button,select{font:inherit;color:inherit;min-height:44px;border:1px solid #bdcfc3;background:white;border-radius:12px;padding:9px 13px;cursor:pointer}button:disabled{opacity:.45;cursor:default}button:focus-visible,select:focus-visible,input:focus-visible{outline:3px solid #c68c2a;outline-offset:3px}h2{font-size:27px;line-height:1.3;margin:18px 0 8px}h3{font-size:18px;margin:0 0 12px}.summary{color:var(--muted);font-size:14px}.card{background:var(--paper);border:1px solid var(--line);border-radius:20px;padding:24px;margin-top:18px}.toolbar{display:flex;align-items:center;gap:16px;justify-content:space-between;flex-wrap:wrap}.toolbar select{max-width:100%;min-width:0}.toolbar label{display:flex;gap:8px;align-items:center}input{width:20px;height:20px;accent-color:var(--accent)}.members{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}.member{display:grid;font-size:14px}.member span{color:var(--muted)}.english{font:21px/1.8 Georgia,serif;margin:0 0 12px}.translation{border-left:3px solid #d7b66a;padding-left:14px;color:var(--muted);margin:8px 0 22px}.paragraph+.paragraph{margin-top:26px}#counter{font-size:14px;color:var(--accent);font-weight:700}#question{font:27px/1.5 Georgia,serif;margin:20px 0}.buttons{display:flex;gap:10px;flex-wrap:wrap}.primary{background:var(--accent);color:white;border-color:var(--accent)}#status{min-height:52px;color:var(--muted);font-size:14px}#answerBox{background:#eaf3e9;padding:18px;border-radius:14px;margin:18px 0}#answerText{font:21px/1.6 Georgia,serif;margin:8px 0}#source{margin-top:18px;border-top:1px solid var(--line);padding-top:18px}.bottom{display:flex;justify-content:space-between;margin-top:24px;gap:12px}details>summary{cursor:pointer;font-weight:700}#vocabulary{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px;margin-top:18px}.word{border-top:1px solid var(--line);padding-top:12px;overflow-wrap:anywhere}.word strong{font-size:18px}.ipa{display:block;color:var(--accent)}.word p{font-size:14px;margin:4px 0}.note{color:var(--muted);font-size:13px;margin-top:24px}p,button,select{overflow-wrap:anywhere}[hidden]{display:none!important}@media(max-width:480px){main{padding:20px 14px 32px}.card{padding:18px;border-radius:16px}.members{grid-template-columns:repeat(2,minmax(0,1fr))}.english{font-size:20px}h2{font-size:24px}#question{font-size:24px}.lessons button{font-size:14px;padding:10px}#vocabulary{grid-template-columns:1fr}}
</style></head><body><main>
<header><div class="logo" aria-hidden="true">DF</div><div><h1>Deep Flow · Giáo trình mẫu</h1><p>Đọc · Hiểu · Phản xạ</p></div></header>
<span class="badge">Bản nháp chờ duyệt · Chưa có giọng đọc</span>
<nav id="lessonNav" class="lessons" aria-label="Chọn bài"></nav>
<h2 id="lessonTitle"></h2><p id="summary" class="summary"></p>
<aside id="family" class="card"><h3>Gia đình David · 5 thành viên</h3><div id="members" class="members"></div></aside>
<section class="card"><div class="toolbar"><select id="sections" aria-label="Chọn phần bài học"></select><label><input id="bilingual" type="checkbox">Song ngữ</label></div></section>
<article class="card"><h3 id="sectionTitle"></h3><div id="story"></div>
<div id="practice" hidden><p id="counter"></p><h4 id="question"></h4>
<div class="buttons"><button id="wait" class="primary"></button><button id="reveal">Xem câu trả lời mẫu</button></div>
<p id="status" role="status" aria-live="polite"></p>
<div id="answerBox" hidden><strong>Câu trả lời mẫu</strong><p id="answerText"></p></div>
<button id="sourceButton">Xem đoạn truyện</button><div id="source" hidden></div>
<div class="bottom"><button id="previous">← Câu trước</button><button id="next">Câu tiếp →</button></div></div></article>
<details class="card"><summary>${inventory.words.length} từ/cụm đề xuất và IPA</summary><p class="summary">Chưa phải danh sách chuẩn 3.000 từ. Nghĩa và IPA đang chờ rà soát.</p><div id="vocabulary"></div></details>
<p class="note">Bản đọc thử không có audio, không ghi âm hoặc chấm điểm, không lưu tiến độ. Khoảng chờ chỉ để bạn thử trả lời bằng cách đọc câu hỏi; thời lượng giọng đọc vẫn chưa được đo.</p>
</main><script id="payload" type="application/json">${payload}</script><script>${js}</script></body></html>\n`;
}
module.exports={renderPreview};
