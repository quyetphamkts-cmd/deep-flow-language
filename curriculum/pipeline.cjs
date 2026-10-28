'use strict';
const {normalizeManifest, validateHandoff} = require('./manifest.cjs');
const {validateScript, auditCurriculum, planAudio} = require('./engine.cjs');

function createPlaceholder(manifest) {
  const m = normalizeManifest(manifest);
  return {schemaVersion:1, status:'placeholder_not_authored', scripts:m.lessons.filter(l=>['C01','Z01'].includes(l.id)).flatMap(l=>l.components.map(c=>({schemaVersion:1,id:c.id,lessonId:l.id,component:c.kind,version:1,status:'draft',segments:[]})))};
}

// Adapter thử nghiệm: chỉ tạo WAV im lặng để kiểm tra dây chuyền dữ liệu.
// Không phải giọng đọc, không gửi yêu cầu mạng và không dùng API key.
function mockAudio(script, config = {provider:'mock',model:'silence-v1',voice:'test-only',language:'en',speed:1}, cachedKeys = []) {
  if (config.provider !== 'mock') throw new Error('Chưa cho phép nhà cung cấp TTS thật');
  const plan = planAudio(script, config, cachedKeys);
  const durationSeconds = plan.pauseSeconds + script.segments.filter(s=>s.type!=='pause').length * 0.1;
  if (!Number.isFinite(durationSeconds) || durationSeconds <= 0 || durationSeconds > 300) throw new Error('Mock chỉ hỗ trợ đoạn thử >0 và <=300 giây');
  const samples = Math.ceil(durationSeconds * 8000), pcmSize = samples * 2, wav = Buffer.alloc(44 + pcmSize);
  wav.write('RIFF'); wav.writeUInt32LE(36 + pcmSize,4); wav.write('WAVEfmt ',8); wav.writeUInt32LE(16,16); wav.writeUInt16LE(1,20); wav.writeUInt16LE(1,22); wav.writeUInt32LE(8000,24); wav.writeUInt32LE(16000,28); wav.writeUInt16LE(2,32); wav.writeUInt16LE(16,34); wav.write('data',36); wav.writeUInt32LE(pcmSize,40);
  return {plan, wav, metadata:{status:'mock_not_for_publication',scriptId:script.id,scriptVersion:script.version,durationSeconds:samples/8000,cacheKey:plan.cacheKey,provider:'mock',cost:0,narration:false}};
}

function preview(manifest, catalog, scripts) {
  const errors = validateHandoff(manifest); if (errors.length) throw new Error(errors.join('\n'));
  const m = normalizeManifest(manifest), audit = auditCurriculum(manifest,catalog,scripts), sections = [];
  for (const l of m.lessons.filter(l=>['C01','Z01'].includes(l.id))) {
    for (const c of l.components) {
      const s = scripts.find(s=>s.id===c.id);
      if (!s) { sections.push({lessonId:l.id,section:c.kind,status:'missing_script'}); continue; }
      const errs = validateScript(s,l,c); if (errs.length) throw new Error(errs.join('\n'));
      sections.push({lessonId:l.id,section:c.kind,status:s.segments.length?'draft':'awaiting_chatgpt_script',scriptVersion:s.version,segments:structuredClone(s.segments),audio:null,subtitles:null});
    }
  }
  return {schemaVersion:1,mode:'offline-preview',publicationReady:false,vocabularyReady:audit.vocabularyReady,missingCanonicalVocabulary:!audit.catalogApproved || audit.catalogSize!==3000,sections,notes:['Chưa có audio thật hoặc đồng bộ phụ đề.','Không ghi vào IndexedDB, localStorage hoặc giao diện app.','Mock audio là WAV im lặng, không đo thời lượng giọng đọc.']};
}
module.exports = {createPlaceholder,mockAudio,preview};
