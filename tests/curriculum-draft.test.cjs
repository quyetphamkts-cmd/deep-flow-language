const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {reviewDraft,renderDraft}=require('../curriculum/draft-review.cjs');
const manifest=require('../docs/curriculum/curriculum_manifest.json');
const bundle=require('../curriculum/drafts/v1/scripts.json');
const inventory=require('../curriculum/drafts/v1/vocabulary.proposed.json');
const clone=v=>structuredClone(v);

test('authored lessons cover all sections with question sources and remains unapproved',()=>{
  const r=reviewDraft(manifest,bundle,inventory);
  assert.equal(r.sections.length,14);assert.equal(r.structuralQaPassed,true);
  assert.equal(r.lessons[0].questionCount,69);assert.equal(r.lessons.find(l=>l.id==='C02').questionCount,64);assert.equal(r.lessons.find(l=>l.id==='Z01').questionCount,40);
  assert.equal(r.publicationReady,false);assert.equal(r.canonicalVocabularyReady,false);
  assert.equal(r.proposedInventoryAudit.vocabularyReady,false);
  assert.equal(r.proposedInventoryAudit.emptyScripts.length,0);
  assert.ok(r.sections.every(s=>s.measuredAudioSeconds===null));
});
test('invalid or cross-lesson question references fail, so optional expansion cannot become a prerequisite',()=>{
  for(const ref of [{scriptId:'C01-main_story',segmentId:'missing'},{scriptId:'Z01-daily_adventure',segmentId:'s01'}]){
    const b=clone(bundle),q=b.scripts.find(s=>s.component==='quick_response').segments.find(s=>s.type==='question');q.sourceRefs=[ref];
    assert.throws(()=>reviewDraft(manifest,b,inventory),/liên kết truyện sai/);
  }
  const b=clone(bundle);delete b.scripts.find(s=>s.component==='quick_response').segments.find(s=>s.type==='question').sourceRefs;
  assert.throws(()=>reviewDraft(manifest,b,inventory),/thiếu liên kết truyện/);
});
test('answer immediately follows its question, with explicit pause and non-exact-match policy',()=>{
  for(const mutation of [q=>q.answerPauseMs=-1,q=>delete q.answerPolicy,q=>q.answerId='missing']){
    const b=clone(bundle),q=b.scripts.find(s=>s.component==='quick_response').segments.find(s=>s.type==='question');mutation(q);
    assert.throws(()=>reviewDraft(manifest,b,inventory));
  }
  const b=clone(bundle),s=b.scripts.find(s=>s.component==='quick_response');[s.segments[2],s.segments[4]]=[s.segments[4],s.segments[2]];
  assert.throws(()=>reviewDraft(manifest,b,inventory),/ngay sau/);
});
test('empty, duplicate or prematurely approved draft sections are rejected',()=>{
  for(const mutation of [b=>b.scripts[0].segments=[],b=>b.scripts[0].status='approved',b=>b.scripts.push(clone(b.scripts[0]))]){
    const b=clone(bundle);mutation(b);assert.throws(()=>reviewDraft(manifest,b,inventory));
  }
});
test('Word Discovery selections need a real inventory entry, an explanation and occurrence in section',()=>{
  const b=clone(bundle);b.wordDiscoverySelection.push({wordId:'en.absent',reason:'test'});
  assert.throws(()=>reviewDraft(manifest,b,inventory),/Word Discovery/);
  const c=clone(bundle);c.wordDiscoverySelection[0].reason='';assert.throws(()=>reviewDraft(manifest,c,inventory),/Word Discovery/);
});
test('planning includes all explicit learner pauses but does not count translations as narrated words',()=>{
  const before=reviewDraft(manifest,bundle,inventory),b=clone(bundle);
  b.scripts[0].segments.push({id:'timing-test',type:'pause',seconds:1});
  b.scripts[0].segments[0].translationVi='extra translated text '.repeat(1000);
  const after=reviewDraft(manifest,b,inventory);
  assert.equal(after.lessons[0].spokenWords,before.lessons[0].spokenWords);
  assert.equal(after.lessons[0].pauseSeconds,before.lessons[0].pauseSeconds+1);
  assert.equal(after.lessons[0].estimatedSecondsAt110Wpm,before.lessons[0].estimatedSecondsAt110Wpm+1);
});
test('Quick Response uses exactly three seconds and permits short non-exact responses throughout',()=>{
  assert.equal(bundle.quickResponsePolicy.fullSentenceRequired,false);assert.equal(bundle.quickResponsePolicy.exactWordingRequired,false);
  const questions=bundle.scripts.flatMap(s=>s.segments).filter(s=>s.type==='question');
  assert.equal(questions.length,237);assert.ok(questions.every(q=>q.answerPauseMs===3000));
  for(const ms of [0,4500,6000]){
    const b=clone(bundle);b.scripts.find(s=>s.component==='quick_response').segments.find(s=>s.type==='question').answerPauseMs=ms;
    assert.throws(()=>reviewDraft(manifest,b,inventory),/3 giây/);
  }
});
test('proposed inventory is actually present and core vocabulary reappears in the independent expansion',()=>{
  const r=reviewDraft(manifest,bundle,inventory),a=r.proposedInventoryAudit;
  assert.equal(inventory.status,'proposed_not_canonical');assert.equal(inventory.license,null);
  assert.deepEqual(a.missingWords,[]);assert.equal(r.proposedVocabularyCount,72);assert.equal(r.wordDiscoveryCount,27);
  const z=a.lessons.find(l=>l.id==='Z01');assert.ok(z.reviewed.includes('en.repair'));assert.ok(z.reviewed.includes('en.tool'));assert.ok(z.reviewed.includes('en.reach'));
});
test('human review document stays synchronized with structured English/Vietnamese scripts',()=>{
  const actual=fs.readFileSync(path.join(__dirname,'../docs/curriculum/C01_Z01_DRAFT_V1.md'),'utf8');
  assert.equal(actual,renderDraft(manifest,bundle,inventory,{lessonIds:['C01','Z01']})+'\n');
  for(const s of bundle.scripts.filter(s=>s.component!=='quick_response'))assert.ok(s.segments.every(p=>p.translationVi?.trim()));
});
test('C02 has its own Word Discovery and rejects missing parts, sources or selections',()=>{
  const r=reviewDraft(manifest,bundle,inventory);assert.deepEqual(r.wordDiscoveryByLesson,{C01:9,C02:9,C03:9});
  for(const mutation of [b=>b.scripts=b.scripts.filter(s=>s.id!=='C02-time_shift'),b=>delete b.wordDiscoveryByLesson.C02,b=>b.wordDiscoveryByLesson.C02[0].wordId='en.ribbon']){
    const b=clone(bundle);mutation(b);assert.throws(()=>reviewDraft(manifest,b,inventory));
  }
  const b=clone(bundle),q=b.scripts.find(s=>s.id==='C02-quick_response').segments.find(p=>p.type==='question');
  q.sourceRefs=[{scriptId:'C01-main_story',segmentId:'s01'}];assert.throws(()=>reviewDraft(manifest,b,inventory),/liên kết truyện sai/);
});
test('C02 read export includes just that lesson while preview reports every authored lesson',()=>{
  const actual=fs.readFileSync(path.join(__dirname,'../docs/curriculum/C02_DRAFT_V1.md'),'utf8');
  assert.equal(actual,renderDraft(manifest,bundle,inventory,{lessonIds:['C02']})+'\n');
  assert.ok(actual.includes('The House That Listened'));assert.ok(!actual.includes('## Z01'));
  assert.throws(()=>renderDraft(manifest,bundle,inventory,{lessonIds:['C04']}));
  const {preview}=require('../curriculum/pipeline.cjs');const p=preview(manifest,inventory,bundle.scripts);
  assert.equal(p.sections.length,14);assert.equal(p.sections.filter(s=>s.lessonId==='C02').length,4);
});
test('C03 exports a complete draft, with correct Q&A references and Word Discovery',()=>{
  const r=reviewDraft(manifest,bundle,inventory),s=bundle.scripts.filter(s=>s.lessonId==='C03');
  assert.equal(s.length,4);assert.equal(r.lessons.find(l=>l.id==='C03').questionCount,64);
  const actual=fs.readFileSync(path.join(__dirname,'../docs/curriculum/C03_DRAFT_V1.md'),'utf8');
  assert.equal(actual,renderDraft(manifest,bundle,inventory,{lessonIds:['C03']})+'\n');
  assert.ok(!actual.includes('## C02'));assert.equal(r.wordDiscoveryByLesson.C03,9);
  const b=clone(bundle),q=b.scripts.find(s=>s.id==='C03-quick_response').segments.find(q=>q.type==='question');
  q.sourceRefs=[{scriptId:'C02-main_story',segmentId:'s01'}];assert.throws(()=>reviewDraft(manifest,b,inventory),/liên kết truyện sai/);
  const missing=clone(bundle);missing.scripts=missing.scripts.filter(s=>s.id!=='C03-word_discovery');assert.throws(()=>reviewDraft(manifest,missing,inventory));
});
