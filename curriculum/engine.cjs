'use strict';

// Công cụ biên soạn độc lập. Không đọc/ghi dữ liệu người học hoặc gọi mạng.
const crypto = require('node:crypto');
const {validateHandoff, normalizeManifest} = require('./manifest.cjs');
const COMPONENTS = Object.freeze({core: ['main_story', 'word_discovery', 'quick_response', 'time_shift'], zodiac: ['daily_adventure', 'quick_response']});
const text = value => typeof value === 'string' && value.trim().length > 0;
const version = value => Number.isSafeInteger(value) && value > 0;
const clone = value => structuredClone(value);
const normalize = value => String(value).normalize('NFKC').toLowerCase().replace(/[’‘]/g, "'").trim().replace(/\s+/g, ' ');
const tokens = value => normalize(value).match(/[a-z]+(?:'[a-z]+)*/g) || [];

function validateManifest(m) { return validateHandoff(m); }

function createVocabularyIndex(catalog) {
  if (!catalog || catalog.schemaVersion !== 1 || !version(catalog.version) || !Array.isArray(catalog.words)) throw new Error('Danh sách từ không đúng schema');
  const words = new Map(), forms = new Map();
  for (const w of catalog.words) {
    if (!text(w.id) || words.has(w.id) || !text(w.lemma) || !Array.isArray(w.forms)) throw new Error('Từ thiếu ID/lemma/forms hoặc trùng ID');
    words.set(w.id, clone(w));
    for (const f of [w.lemma, ...w.forms]) {
      if (!text(f) || tokens(f).join(' ') !== normalize(f)) throw new Error(`Dạng từ không hỗ trợ: ${w.id}`);
      const key = normalize(f);
      if (forms.has(key) && forms.get(key) !== w.id) throw new Error(`Dạng từ mơ hồ: ${key}; cần người biên soạn giải quyết`);
      forms.set(key, w.id);
    }
  }
  return {catalog: clone(catalog), words, forms, maxTokens: Math.max(1, ...[...forms.keys()].map(f => tokens(f).length))};
}

function validateScript(script, lesson, component) {
  const errors = [];
  if (!script || script.schemaVersion !== 1 || !text(script.id) || script.lessonId !== lesson.id || script.component !== component.kind || !version(script.version) || !['draft', 'review', 'approved'].includes(script.status)) return [`${component.id}: kịch bản không đúng schema`];
  if (!Array.isArray(script.segments)) return [`${script.id}: thiếu segments`];
  const ids = new Set();
  for (const s of script.segments) {
    if (!s || !text(s.id) || ids.has(s.id) || !['narration', 'question', 'answer', 'pause'].includes(s.type)) { errors.push(`${script.id}: đoạn sai/trùng ID`); continue; }
    ids.add(s.id);
    if (s.type === 'pause') {
      if (!Number.isFinite(s.seconds) || s.seconds <= 0 || s.seconds > 120) errors.push(`${s.id}: khoảng dừng phải >0 và <=120 giây`);
    } else if (!text(s.text)) errors.push(`${s.id}: thiếu văn bản`);
    if (s.type === 'question' && (!text(s.answerId) || !script.segments.some(a => a?.id === s.answerId && a.type === 'answer'))) errors.push(`${s.id}: thiếu đáp án liên kết`);
    if (s.type === 'question' && (!Number.isFinite(s.answerPauseMs) || s.answerPauseMs < 0 || s.answerPauseMs > 120000)) errors.push(`${s.id}: thiếu thời gian trả lời hợp lệ (0–120000 ms)`);
  }
  if (component.kind === 'quick_response' && script.segments.length && !script.segments.some(s => s?.type === 'question')) errors.push(`${script.id}: Quick Response thiếu câu hỏi`);
  if (script.id !== component.id) errors.push(`${component.id}: ID kịch bản phải khớp thành phần`);
  if (script.status === 'approved' && (!text(script.approvedBy) || !text(script.approvedAt) || !Number.isFinite(Date.parse(script.approvedAt)) || !script.segments.length)) errors.push(`${script.id}: thiếu nội dung/thông tin phê duyệt`);
  return errors;
}

// Ghép dạng từ dài nhất trước. Không đoán gốc từ (saw/see/saw cần quy tắc đã duyệt).
// Tách theo dấu câu để cụm từ không bị ghép xuyên qua hai câu.
function scanText(value, index) {
  const result = [];
  for (const sentence of String(value).split(/[.!?;:,\n\r]+/)) {
    const ts = tokens(sentence);
    for (let i = 0; i < ts.length;) {
      let match;
      for (let n = Math.min(index.maxTokens, ts.length - i); n > 0; n--) {
        const form = ts.slice(i, i + n).join(' '), id = index.forms.get(form);
        if (id) { match = {id, form, length: n}; break; }
      }
      result.push(match || {id: null, form: ts[i], length: 1});
      i += match?.length || 1;
    }
  }
  return result;
}

function auditCurriculum(manifest, catalog, scripts = []) {
  const errors = validateManifest(manifest);
  if (errors.length) throw new Error(errors.join('\n'));
  manifest = normalizeManifest(manifest);
  const index = createVocabularyIndex(catalog), scriptMap = new Map();
  for (const s of scripts) { if (!s || !text(s.id) || scriptMap.has(s.id)) throw new Error('Kịch bản thiếu/trùng ID'); scriptMap.set(s.id, s); }
  const seen = new Map(), unknown = new Map(), assigned = new Map(), lessons = [], missingScripts = [], emptyScripts = [], usedScripts = new Set(), unallocated = [];
  for (const l of manifest.lessons) {
    const counts = new Map();
    for (const c of l.components) {
      const scriptId = c.script || c.id;
      if (!scriptMap.has(scriptId)) { missingScripts.push(c.id); continue; }
      const s = scriptMap.get(scriptId); usedScripts.add(s.id);
      const errs = validateScript(s, l, c); if (errs.length) throw new Error(errs.join('\n'));
      if (!s.segments.length) emptyScripts.push(s.id);
      for (const seg of s.segments) {
        if (seg.type === 'pause') continue;
        for (const hit of scanText(seg.text, index)) {
          if (hit.id) counts.set(hit.id, (counts.get(hit.id) || 0) + 1);
          else unknown.set(hit.form, (unknown.get(hit.form) || 0) + 1);
        }
      }
    }
    for (const id of l.targetWordIds) {
      if (!index.words.has(id)) errors.push(`${l.id}: từ mục tiêu không có trong danh sách: ${id}`);
      if (assigned.has(id)) errors.push(`${id}: phân bổ từ mới trùng ở ${assigned.get(id)} và ${l.id}`);
      else assigned.set(id, l.id);
    }
    const introduced = [], reviewed = [];
    for (const [id, count] of counts) {
      if (!seen.has(id)) { introduced.push(id); seen.set(id, {id, firstLesson: l.id, occurrences: 0, lessons: []}); }
      else reviewed.push(id);
      const w = seen.get(id); w.occurrences += count; w.lessons.push(l.id);
    }
    lessons.push({id: l.id, introduced, reviewed, counts: Object.fromEntries(counts), missingTargets: l.targetWordIds.filter(id => !counts.has(id)), targetAllocation: l.targetWordIds.length, plannedNewWords: l.targetNewWords});
  }
  for (const id of index.words.keys()) if (!assigned.has(id)) unallocated.push(id);
  const missingWords = [...index.words.keys()].filter(id => !seen.has(id));
  const coreSeen = new Set(lessons.filter(l=>l.id.startsWith('C')).flatMap(l=>Object.keys(l.counts)));
  const missingCoreWords = [...index.words.keys()].filter(id=>!coreSeen.has(id));
  const catalogApproved = catalog.status === 'approved' && text(catalog.source) && text(catalog.license);
  // Chỉ là độ sẵn sàng từ vựng, không phải cổng xuất bản giáo trình.
  const vocabularyReady = catalogApproved && index.words.size === 3000 && !unallocated.length && !missingCoreWords.length && !errors.length && lessons.filter(l=>l.id.startsWith('C')).every(l=>l.targetAllocation===250 && !l.missingTargets.length);
  return {schemaVersion: 1, catalogSize: index.words.size, targetSize: manifest.targetVocabularySize, catalogApproved, vocabularyReady, lessons, words: [...seen.values()], missingWords, missingCoreWords, unallocated, outsideVocabulary: [...unknown].sort((a,b) => b[1]-a[1] || a[0].localeCompare(b[0])).map(([form,count]) => ({form,count})), missingScripts, emptyScripts, unusedScripts: scripts.filter(s => !usedScripts.has(s.id)).map(s => s.id), errors};
}

// Chỉ ghi nhận bằng chứng trả lời, không suy diễn trí nhớ từ số lần xuất hiện.
function recordEvidence(state, {wordId, skill, rating, at}) {
  if (!text(wordId) || !['recognition','understanding','use'].includes(skill) || !['again','hard','good','easy'].includes(rating) || !Number.isFinite(at) || at < 0) throw new Error('Bằng chứng học không hợp lệ');
  const next = clone(state || {}), entry = Object.hasOwn(next, wordId) ? next[wordId] : {skills: {}};
  const prev = entry.skills[skill] || {reviews: 0, lapses: 0, intervalDays: 0};
  if (prev.lastReviewed !== undefined && at <= prev.lastReviewed) throw new Error('Bằng chứng cũ/trùng thời điểm');
  const intervalDays = rating === 'again' ? 0 : rating === 'hard' ? 1 : rating === 'easy' ? Math.max(7, prev.intervalDays * 3) : prev.intervalDays < 1 ? 1 : prev.intervalDays < 3 ? 3 : Math.max(7, prev.intervalDays * 2);
  entry.skills[skill] = {reviews: prev.reviews + 1, lapses: prev.lapses + (rating === 'again' ? 1 : 0), intervalDays, lastReviewed: at, lastRating: rating, due: at + (rating === 'again' ? 600000 : intervalDays * 86400000)};
  Object.defineProperty(next, wordId, {value:entry, enumerable:true, writable:true, configurable:true}); return next;
}
function reviewQueue(state, now) {
  if (!Number.isFinite(now)) throw new Error('Thời điểm không hợp lệ');
  return Object.entries(state).flatMap(([wordId,e]) => Object.entries(e.skills || {}).filter(([,s])=>s.due<=now).map(([skill,s])=>({wordId,skill,...s}))).sort((a,b)=>b.lapses-a.lapses || a.due-b.due || a.wordId.localeCompare(b.wordId));
}

// Kế hoạch TTS offline: khóa cache thay đổi theo toàn bộ nội dung và cấu hình giọng.
// Không có API adapter thực thi trong phiên bản đầu.
function planAudio(script, config, cachedKeys = []) {
  if (!script || !Array.isArray(script.segments) || !script.segments.length || !text(config?.provider) || !text(config.model) || !text(config.voice) || !text(config.language) || !Number.isFinite(config.speed) || config.speed < 0.5 || config.speed > 2) throw new Error('Thiếu kịch bản/cấu hình TTS hợp lệ');
  const allowed = ['provider','model','voice','language','speed'];
  if (Object.keys(config).some(k=>!allowed.includes(k))) throw new Error('Cấu hình kế hoạch chỉ nhận provider/model/voice/language/speed; không nhận credential');
  for (const s of script.segments) {
    if (!s || !text(s.id) || !['narration','question','answer','pause'].includes(s.type)) throw new Error('Đoạn TTS không hợp lệ');
    if (s.type==='pause' ? !Number.isFinite(s.seconds) || s.seconds<=0 || s.seconds>120 : !text(s.text)) throw new Error('Thiếu văn bản/khoảng dừng TTS');
    if (s.type==='question' && (!Number.isFinite(s.answerPauseMs) || s.answerPauseMs<0 || s.answerPauseMs>120000)) throw new Error('Khoảng trả lời TTS không hợp lệ');
  }
  const settings = Object.fromEntries(allowed.map(k=>[k,config[k]]));
  const payload = {schemaVersion:1,scriptId:script.id,scriptVersion:script.version,segments:script.segments,settings};
  const cacheKey = crypto.createHash('sha256').update(JSON.stringify(payload)).digest('hex');
  return {mode:'dry-run',cacheKey,cached:cachedKeys.includes(cacheKey),characters:script.segments.reduce((n,s)=>n+(s.type==='pause'?0:Array.from(s.text||'').length),0),pauseSeconds:script.segments.reduce((n,s)=>n+(s.type==='pause'?s.seconds:s.type==='question'?s.answerPauseMs/1000:0),0),settings,executionEnabled:false};
}

module.exports = {COMPONENTS, validateManifest, validateScript, createVocabularyIndex, scanText, auditCurriculum, recordEvidence, reviewQueue, planAudio};
