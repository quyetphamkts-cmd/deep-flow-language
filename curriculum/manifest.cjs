'use strict';
const schema = require('./manifest.schema.json');
// Các quy tắc JSON Schema được dùng trong file cục bộ.
// Không tải schema từ mạng hoặc chạy mã trong tài liệu.
function checkSchema(value, rule, path = '$') {
  const errors = [], fail = msg => errors.push(`${path}: ${msg}`);
  if (rule.anyOf) {
    if (!rule.anyOf.some(r => checkSchema(value,r,path).length === 0)) fail('không khớp cấu trúc cho phép');
    return errors;
  }
  if (Object.hasOwn(rule,'const') && value !== rule.const) fail(`cần ${JSON.stringify(rule.const)}`);
  if (rule.enum && !rule.enum.includes(value)) fail('giá trị ngoài danh sách');
  if (rule.type) {
    const ok = rule.type === 'object' ? value !== null && typeof value === 'object' && !Array.isArray(value) : rule.type === 'array' ? Array.isArray(value) : rule.type === 'integer' ? Number.isInteger(value) : typeof value === rule.type;
    if (!ok) return [...errors,`${path}: type ${rule.type} bắt buộc`];
  }
  if (typeof value === 'string') {
    if (rule.minLength && value.trim().length < rule.minLength) fail('văn bản trống');
    if (rule.pattern && !new RegExp(rule.pattern).test(value)) fail('định dạng sai');
  }
  if (Array.isArray(value)) {
    if (rule.minItems !== undefined && value.length < rule.minItems || rule.maxItems !== undefined && value.length > rule.maxItems) fail('số mục sai');
    if (rule.uniqueItems && new Set(value.map(v=>JSON.stringify(v))).size !== value.length) fail('trùng mục');
    if (rule.items) value.forEach((v,i)=>errors.push(...checkSchema(v,rule.items,`${path}[${i}]`)));
  } else if (value !== null && typeof value === 'object') {
    for (const k of rule.required || []) if (!Object.hasOwn(value,k)) fail(`thiếu trường: ${k}`);
    for (const [k,v] of Object.entries(value)) {
      if (rule.properties && Object.hasOwn(rule.properties,k)) errors.push(...checkSchema(v,rule.properties[k],`${path}.${k}`));
      else if (rule.additionalProperties === false) fail(`trường không hỗ trợ: ${k}`);
    }
  }
  return errors;
}
function validateHandoff(m) {
  const errors = checkSchema(m,schema);
  if (errors.length) return errors;
  const animals = ['Mouse','Buffalo','Tiger','Cat','Dragon','Snake','Horse','Goat','Monkey','Rooster','Dog','Pig'];
  const slugs = ['Ty','Suu','Dan','Mao','Thin','Ty_Snake','Ngo','Mui','Than','Dau','Tuat','Hoi'];
  const coreKinds = ['main_story','word_discovery','quick_response','time_shift'], zodiacKinds = ['daily_adventure','quick_response'];
  const seen = new Set();
  m.lessons.forEach((l,i)=>{
    const core = i<12, n = core ? i+1 : i-11, group = Math.floor((n-1)/5), expected = `${core?'C':'Z'}${String(n).padStart(2,'0')}`;
    if (l.id !== expected || seen.has(l.id) || l.type !== (core?'core':'optional_expansion')) errors.push(`${l.id}: thứ tự/loại/ID sai (cần ${expected})`);
    seen.add(l.id);
    if (JSON.stringify(l.sections.map(s=>s.id)) !== JSON.stringify(core?coreKinds:zodiacKinds)) errors.push(`${l.id}: thành phần sai`);
    if (!core && (l.aligned_core_lesson !== `C${String(group+1).padStart(2,'0')}` || l.zodiac.animal !== animals[group] || l.zodiac.vietnamese_slug !== slugs[group])) errors.push(`${l.id}: liên kết bài chính/con giáp sai`);
  });
  return errors;
}
function normalizeManifest(m) {
  const errors = validateHandoff(m); if (errors.length) throw new Error(errors.join('\n'));
  return {schemaVersion:1, version:1, id:'architect-zodiac', language:m.language, targetVocabularySize:m.vocabulary_target_unique_lemmas,
    lessons:m.lessons.map((l,i)=>({id:l.id, order:i+1, kind:l.type==='core'?'core':'zodiac', title:l.title, version:1, status:l.status,
      optional:l.type!=='core', linkedCore:l.aligned_core_lesson || null, targetNewWords:l.target_new_words || 0, targetWordIds:l.word_ids || [],
      targetDurationSeconds:l.type==='core'?{min:l.target_audio_minutes.preferred*60,max:l.target_audio_minutes.maximum*60}:{min:900,max:900},
      components:l.sections.map(s=>({id:`${l.id}-${s.id}`,kind:s.id,script:null,audio:null}))}))};
}
module.exports = {checkSchema,validateHandoff,normalizeManifest};
