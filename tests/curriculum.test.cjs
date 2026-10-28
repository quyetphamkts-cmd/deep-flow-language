const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {spawnSync} = require('node:child_process');
const e = require('../curriculum/engine.cjs');
const {normalizeManifest,checkSchema} = require('../curriculum/manifest.cjs');
const {createPlaceholder,preview,mockAudio} = require('../curriculum/pipeline.cjs');
const manifest = require('../docs/curriculum/curriculum_manifest.json');
const pending = require('../curriculum/vocabulary.pending.json');
const root = path.resolve(__dirname,'..');
const copy = v => structuredClone(v);
// Các từ/văn bản dưới đây chỉ là dữ liệu kiểm thử, không phải giáo trình được duyệt.
const catalog = {schemaVersion:1,version:1,status:'test_only',source:null,license:null,words:[{id:'house',lemma:'house',forms:['houses']},{id:'go',lemma:'go',forms:['went','gone']},{id:'come-up',lemma:'come up with',forms:[]},{id:'unused',lemma:'unused',forms:[]}]};
function script(id,text='Houses went. Come up with. Surprise!') {
  const [lessonId,...parts]=id.split('-'); return {schemaVersion:1,id,lessonId,component:parts.join('-'),version:1,status:'draft',segments:[{id:'s1',type:'narration',text}]};
}
function qa(id) {const s=script(id);s.segments=[{id:'q1',type:'question',text:'House?',answerId:'a1',answerPauseMs:500},{id:'a1',type:'answer',text:'House.'}];return s;}

test('ZIP manifest: 12 core, 60 optional expansions, 168 sections, correct Cat mapping',()=>{
  assert.deepEqual(e.validateManifest(manifest),[]);
  const m=normalizeManifest(manifest);
  assert.equal(m.lessons.length,72);assert.equal(m.lessons.flatMap(l=>l.components).length,168);
  assert.equal(manifest.lessons.find(l=>l.id==='Z16').zodiac.animal,'Cat');
  assert.equal(m.lessons.filter(l=>l.optional).length,60);
});
test('reject missing/duplicate IDs, bad counts, core alignment, zodiac, section order and durations',()=>{
  const mutations=[m=>m.lessons.pop(),m=>m.lessons[1].id='C01',m=>m.core_count=13,m=>m.lessons[12].aligned_core_lesson='C02',m=>m.lessons[27].zodiac.animal='Rabbit',m=>m.lessons[0].sections.reverse(),m=>m.lessons[0].target_audio_minutes.maximum=90,m=>m.lessons[12].independently_comprehensible=false,m=>m.lessons[0].word_ids=['x','x'],m=>m.lessons[0].unknown=true,m=>m.lessons[1]=null];
  for(const mutate of mutations){const m=copy(manifest);mutate(m);assert.ok(e.validateManifest(m).length);}
});
test('JSON Schema subset validates required fields, arrays, type, enum, const, pattern, anyOf',()=>{
  assert.ok(checkSchema({}, {type:'object',required:['x']}).length);
  assert.ok(checkSchema([1,1],{type:'array',uniqueItems:true,minItems:3}).length);
  assert.ok(checkSchema('  ',{type:'string',minLength:1}).length);
  assert.ok(checkSchema('Z99',{type:'string',pattern:'^C'}).length);
  assert.deepEqual(checkSchema(1,{anyOf:[{const:1},{const:2}]}),[]);
});
test('canonical list absent reports NOT ready and no fabricated coverage',()=>{
  const r=e.auditCurriculum(manifest,pending,createPlaceholder(manifest).scripts);
  assert.equal(r.catalogSize,0);assert.equal(r.catalogApproved,false);assert.equal(r.vocabularyReady,false);
  assert.equal(r.missingScripts.length,162);assert.equal(r.emptyScripts.length,6);assert.equal(r.words.length,0);
});
test('explicit inflections, phrase matching and case normalization; no guessed stems/cross-sentence phrases',()=>{
  const i=e.createVocabularyIndex(catalog);
  const hits=e.scanText('HOUSES went, come up with. come. up with. going.',i);
  assert.deepEqual(hits.filter(h=>h.id).map(h=>h.id),['house','go','come-up']);
  assert.ok(hits.some(h=>h.form==='going'&&h.id===null));
  assert.equal(e.scanText('come, up with',i).some(h=>h.id==='come-up'),false);
});
test('reject ambiguous forms and duplicate IDs instead of silently choosing a lemma',()=>{
  for(const word of [{id:'other',lemma:'went',forms:[]},{id:'house',lemma:'second',forms:[]}]){const c=copy(catalog);c.words.push(word);assert.throws(()=>e.createVocabularyIndex(c));}
});
test('audit first occurrence, review frequency, missing targets, duplicate allocation and out-of-list forms',()=>{
  const m=copy(manifest);m.lessons[0].word_ids=['house','unused'];m.lessons[1].word_ids=['house','not-in-catalog'];
  const r=e.auditCurriculum(m,catalog,[script('C01-main_story'),script('C02-main_story','House. House!')]);
  assert.equal(r.words.find(w=>w.id==='house').firstLesson,'C01');
  assert.equal(r.words.find(w=>w.id==='house').occurrences,3);
  assert.deepEqual(r.lessons[1].reviewed,['house']);assert.deepEqual(r.lessons[0].missingTargets,['unused']);
  assert.ok(r.errors.some(x=>x.includes('trùng')));assert.ok(r.errors.some(x=>x.includes('not-in-catalog')));
  assert.ok(r.outsideVocabulary.some(w=>w.form==='surprise'));assert.deepEqual(r.missingWords,['unused']);
  assert.equal(r.vocabularyReady,false);
});
test('optional expansion exposure does not meet core vocabulary coverage',()=>{
  const r=e.auditCurriculum(manifest,catalog,[script('Z01-daily_adventure','House.')]);
  assert.ok(!r.missingWords.includes('house'));assert.ok(r.missingCoreWords.includes('house'));assert.equal(r.vocabularyReady,false);
});
test('draft scripts accept placeholders; Q&A needs answer and nonnegative explicit pause',()=>{
  const m=normalizeManifest(manifest),l=m.lessons[0],c=l.components.find(c=>c.kind==='quick_response');
  const s=qa(c.id);assert.deepEqual(e.validateScript(s,l,c),[]);
  s.segments[0].answerPauseMs=0;assert.deepEqual(e.validateScript(s,l,c),[]);
  s.segments[0].answerPauseMs=-1;assert.ok(e.validateScript(s,l,c).length);
  delete s.segments[0].answerPauseMs;assert.ok(e.validateScript(s,l,c).length);
  s.segments[0].answerPauseMs=500;s.segments.pop();assert.ok(e.validateScript(s,l,c).length);
  s.segments=[];s.status='approved';assert.ok(e.validateScript(s,l,c).length);
});
test('audit rejects duplicate/mislinked scripts; unused scripts are visible',()=>{
  const s=script('C01-main_story');assert.throws(()=>e.auditCurriculum(manifest,catalog,[s,s]));
  const wrong=copy(s);wrong.lessonId='C02';assert.throws(()=>e.auditCurriculum(manifest,catalog,[wrong]));
  const r=e.auditCurriculum(manifest,catalog,[script('C99-main_story')]);assert.deepEqual(r.unusedScripts,['C99-main_story']);
});
test('learning evidence is separate from exposure, immutable, skill-specific and prioritizes forgotten due items',()=>{
  const empty={};const first=e.recordEvidence(empty,{wordId:'house',skill:'recognition',rating:'good',at:1000});assert.deepEqual(empty,{});
  assert.equal(first.house.skills.recognition.due,86401000);assert.equal(first.house.skills.use,undefined);
  const second=e.recordEvidence(first,{wordId:'go',skill:'use',rating:'again',at:2000});
  assert.deepEqual(e.reviewQueue(second,602001).map(x=>x.wordId),['go']);
  assert.deepEqual(e.reviewQueue(second,86402000).map(x=>x.wordId),['go','house']);
  assert.throws(()=>e.recordEvidence(first,{wordId:'house',skill:'recognition',rating:'good',at:1000}));
});
test('scheduler follows 1,3,7-day good steps and a lapse waits ten minutes',()=>{
  let s={};let at=100;
  for(const days of [1,3,7]){s=e.recordEvidence(s,{wordId:'house',skill:'understanding',rating:'good',at});assert.equal(s.house.skills.understanding.intervalDays,days);at+=1000;}
  s=e.recordEvidence(s,{wordId:'house',skill:'understanding',rating:'again',at});assert.equal(s.house.skills.understanding.due,at+600000);
});
test('C01/Z01 preview is placeholder-only and does not modify manifest',()=>{
  const before=JSON.stringify(manifest),b=createPlaceholder(manifest),p=preview(manifest,pending,b.scripts);
  assert.equal(p.sections.length,6);assert.equal(p.publicationReady,false);assert.ok(p.sections.every(s=>s.audio===null&&s.status==='awaiting_chatgpt_script'));
  assert.equal(JSON.stringify(manifest),before);
  assert.deepEqual(require('../curriculum/scripts.placeholder.json'),b);
});
test('TTS cache includes script, voice, model, speed and pauses; rejects credentials',()=>{
  const s=qa('C01-quick_response'),cfg={provider:'mock',model:'v1',voice:'one',language:'en',speed:1};
  const a=e.planAudio(s,cfg);assert.equal(a.executionEnabled,false);assert.equal(a.pauseSeconds,0.5);assert.equal(e.planAudio(s,cfg,[a.cacheKey]).cached,true);
  for(const change of [{voice:'two'},{model:'v2'},{speed:1.1}])assert.notEqual(e.planAudio(s,{...cfg,...change}).cacheKey,a.cacheKey);
  const changed=copy(s);changed.segments[0].answerPauseMs=600;assert.notEqual(e.planAudio(changed,cfg).cacheKey,a.cacheKey);
  assert.throws(()=>e.planAudio(s,{...cfg,apiKey:'TEST-NOT-A-SECRET'}));
  assert.throws(()=>mockAudio(s,{...cfg,provider:'real'}));
});
test('mock creates playable silent WAV for all six C01/Z01 sections without calling network',()=>{
  const old=global.fetch;global.fetch=()=>{throw new Error('NETWORK_FORBIDDEN');};
  try {for(const p of createPlaceholder(manifest).scripts){const s=p.component==='quick_response'?qa(p.id):script(p.id,'House.');const a=mockAudio(s);assert.equal(a.wav.toString('ascii',0,4),'RIFF');assert.equal(a.wav.toString('ascii',8,12),'WAVE');assert.equal(a.wav.readUInt32LE(40),a.wav.length-44);assert.equal(a.metadata.cost,0);assert.equal(a.metadata.narration,false);assert.equal(a.metadata.status,'mock_not_for_publication');}}finally{global.fetch=old;}
});
test('CLI validates independently of working directory and fails readiness honestly',()=>{
  const cli=path.join(root,'tools/curriculum/check.cjs');
  const run=(...args)=>spawnSync(process.execPath,[cli,...args],{cwd:'/tmp',encoding:'utf8'});
  const ok=run();assert.equal(ok.status,0,ok.stderr);assert.equal(JSON.parse(ok.stdout).vocabularyReady,false);
  assert.equal(run('--require-vocabulary-ready').status,2);
  assert.equal(run('--preview').status,0);assert.equal(run('--bad').status,1);assert.equal(run('--scripts').status,1);
});
