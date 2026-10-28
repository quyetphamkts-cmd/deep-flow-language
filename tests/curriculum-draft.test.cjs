const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {reviewDraft,renderDraft}=require('../curriculum/draft-review.cjs');
const manifest=require('../docs/curriculum/curriculum_manifest.json');
const bundle=require('../curriculum/drafts/v1/scripts.json');
const inventory=require('../curriculum/drafts/v1/vocabulary.proposed.json');
const clone=v=>structuredClone(v);

test('authored C01/Z01 covers all sections with question sources and remains unapproved',()=>{
  const r=reviewDraft(manifest,bundle,inventory);
  assert.equal(r.sections.length,6);assert.equal(r.structuralQaPassed,true);
  assert.equal(r.lessons[0].questionCount,69);assert.equal(r.lessons[1].questionCount,40);
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
  const before=reviewDraft(manifest,bundle,inventory),b=clone(bundle),s=b.scripts.find(s=>s.component==='quick_response');
  s.segments.find(s=>s.type==='question').answerPauseMs+=1000;
  b.scripts[0].segments[0].translationVi='extra translated text '.repeat(1000);
  const after=reviewDraft(manifest,b,inventory);
  assert.equal(after.lessons[0].spokenWords,before.lessons[0].spokenWords);
  assert.equal(after.lessons[0].pauseSeconds,before.lessons[0].pauseSeconds+1);
  assert.equal(after.lessons[0].estimatedSecondsAt110Wpm,before.lessons[0].estimatedSecondsAt110Wpm+1);
});
test('proposed inventory is actually present and core vocabulary reappears in the independent expansion',()=>{
  const r=reviewDraft(manifest,bundle,inventory),a=r.proposedInventoryAudit;
  assert.equal(inventory.status,'proposed_not_canonical');assert.equal(inventory.license,null);
  assert.deepEqual(a.missingWords,[]);assert.equal(r.proposedVocabularyCount,40);assert.equal(r.wordDiscoveryCount,9);
  const z=a.lessons.find(l=>l.id==='Z01');assert.ok(z.reviewed.includes('en.repair'));assert.ok(z.reviewed.includes('en.tool'));assert.ok(z.reviewed.includes('en.reach'));
});
test('human review document stays synchronized with structured English/Vietnamese scripts',()=>{
  const actual=fs.readFileSync(path.join(__dirname,'../docs/curriculum/C01_Z01_DRAFT_V1.md'),'utf8');
  assert.equal(actual,renderDraft(manifest,bundle,inventory)+'\n');
  for(const s of bundle.scripts.filter(s=>s.component!=='quick_response'))assert.ok(s.segments.every(p=>p.translationVi?.trim()));
});
