'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),crypto=require('node:crypto'),path=require('node:path');
const {auditStandard,makePlan}=require('../curriculum/standard-plan.cjs');
const c=require('../curriculum/vocabulary.standard.json'),p=require('../curriculum/vocabulary.allocations.json'),b=require('../curriculum/drafts/v1/scripts.json');
test('standard list has 3000 single-word heads with pinned source bytes, separate aliases and declared license',()=>{
 const r=auditStandard(c,p,b);assert.equal(r.catalogReady,true);assert.equal(r.allocationReady,true);assert.equal(r.textCoverageReady,false);assert.equal(r.publicationReady,false);assert.equal(r.observedAnywhereInCore,1122);
 for(const s of c.sources)assert.equal(crypto.createHash('sha256').update(fs.readFileSync(path.join(__dirname,'..',s.path))).digest('hex'),s.sha256);
 assert.equal(c.words.filter(w=>w.sourceId==='ngsl-1.01').length,2801);assert.equal(c.words.filter(w=>w.sourceId==='editorial-199').length,199);
 assert.ok(!c.words.some(w=>w.lemma==='aeroplane'));assert.ok(c.words.find(w=>w.lemma==='airplane').forms.includes('aeroplane'));
});
test('allocation is deterministic and covers each ID once with 250 targets per lesson',()=>{
 assert.deepEqual(makePlan(c,b),p);assert.ok(p.lessons.every(l=>l.wordIds.length===250));assert.equal(new Set(p.lessons.flatMap(l=>l.wordIds)).size,3000);
});
test('reject duplicate/unknown targets, chunks, wrong source versions or counts',()=>{
 for(const mutate of [pp=>pp.lessons[1].wordIds[0]=pp.lessons[0].wordIds[0],pp=>pp.lessons[0].wordIds.pop(),pp=>pp.lessons[0].wordIds[0]='absent',pp=>pp.catalogVersion++]){const pp=structuredClone(p);mutate(pp);assert.throws(()=>auditStandard(c,pp,b));}
 const cc=structuredClone(c);cc.words[0].lemma='made up phrase';assert.throws(()=>auditStandard(cc,p,b));
});
test('coverage requires the assigned core lesson; optional stories cannot fill missing targets',()=>{
 const before=auditStandard(c,p,b);const pp=structuredClone(p);let row=before.lessons.find(l=>l.missingTargetIds.length),id=row.missingTargetIds[0];const bb=structuredClone(b);bb.scripts.find(s=>s.lessonId==='Z01').segments.push({id:'z-only',type:'narration',text:c.words.find(w=>w.id===id).lemma});
 assert.equal(auditStandard(c,pp,bb).observedInTargetLesson,before.observedInTargetLesson);
 const cc=structuredClone(b);cc.scripts.find(s=>s.lessonId===row.id&&s.component==='main_story').segments.push({id:'target-added',type:'narration',text:c.words.find(w=>w.id===id).lemma});assert.equal(auditStandard(c,pp,cc).observedInTargetLesson,before.observedInTargetLesson+1);
});
test('embedding uses an isolated lazy frame and is identical to generated source; version advances without native key changes',()=>{
 const {execFileSync}=require('node:child_process');execFileSync(process.execPath,[path.join(__dirname,'../tools/curriculum/import-standard.cjs'),'--check']);execFileSync(process.execPath,[path.join(__dirname,'../tools/curriculum/embed-app.cjs'),'--check']);
 const html=fs.readFileSync(path.join(__dirname,'../www/index.html'),'utf8');assert.match(html,/id="hCurriculum"[^>]*>🎓<\/button><button class="hNews" id="hNews"/);assert.match(html,/sandbox="allow-scripts"/);assert.doesNotMatch(html,/sandbox="[^"]*allow-same-origin/);
});
