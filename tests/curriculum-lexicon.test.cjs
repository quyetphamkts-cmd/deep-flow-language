'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {auditLexicon,renderLexicalAudit}=require('../curriculum/lexical-audit.cjs');
const m=require('../docs/curriculum/curriculum_manifest.json'),b=require('../curriculum/drafts/v1/scripts.json'),v=require('../curriculum/drafts/v1/vocabulary.proposed.json'),f=require('../curriculum/drafts/v1/word-families.proposed.json');
const run=(vv=v,ff=f,bb=b)=>auditLexicon(m,bb,vv,ff);
test('separate single-word lemmas, chunks, inflections and proposed families without claiming readiness',()=>{
 const r=run();assert.equal(r.counts.proposedEntries,173);assert.equal(r.counts.singleWordEntries,171);assert.equal(r.counts.chunks,2);assert.equal(r.counts.proposedMultiMemberFamilies,24);assert.equal(r.counts.classifiedFamilyMembers,53);assert.equal(r.counts.assignedNewTargets,0);assert.equal(r.ready,false);assert.equal(r.canonicalVocabularyReady,false);
 assert.equal(r.counts.surfaceTypes,2126);assert.equal(r.counts.trackedCoreEntries,162);
 const drawing=r.words.find(w=>w.id==='en.drawing');assert.equal(drawing.familyId,'family.draw');assert.ok(drawing.evidence.length);assert.equal(r.words.find(w=>w.id==='en.make-a-difference')?.kind,'chunk');
});
test('family IDs and membership must exist, be unique and cannot include chunks',()=>{
 for(const mutate of [ff=>ff.families.push(structuredClone(ff.families[0])),ff=>ff.families[0].members.push(ff.families[0].members[0]),ff=>ff.families[0].members.push('en.not-in-inventory'),ff=>ff.families[0].members.push('en.make-a-difference')]){const ff=structuredClone(f);mutate(ff);assert.throws(()=>run(v,ff));}
 const ff=structuredClone(f);ff.policy.chunksCountTowardSingleWordTarget=true;assert.throws(()=>run(v,ff));
});
test('evidence is core-only and resolves to a real English source segment; first-seen proposals are not target assignments',()=>{
 const r=run(),ss=new Map(b.scripts.map(s=>[s.id,s]));
 for(const w of r.words)for(const e of w.evidence){assert.match(e.lessonId,/^C\d\d$/);assert.ok(ss.get(e.scriptId).segments.some(s=>s.id===e.segmentId));}
 assert.equal(r.words.find(w=>w.id==='en.ribbon').proposedIntroduction,null);
 assert.ok(r.rows.every(x=>x.assignedNewTargets===0));
 const bb=structuredClone(b);bb.scripts=bb.scripts.filter(s=>s.id!=='C12-time_shift');assert.throws(()=>run(v,f,bb));
});
test('doubling practice increases exposure, not lemma, surface-type, family counts or core lesson coverage',()=>{
 const bb=structuredClone(b);const s=bb.scripts.find(s=>s.id==='C01-quick_response');const more=s.segments.map(x=>({...x,id:'duplicate-'+x.id,...(x.answerId?{answerId:'duplicate-'+x.answerId}:{})}));s.segments.push(...more);
 const before=run(),after=run(v,f,bb);assert.equal(after.counts.surfaceTypes,before.counts.surfaceTypes);assert.equal(after.counts.trackedCoreEntries,before.counts.trackedCoreEntries);assert.equal(after.counts.proposedMultiMemberFamilies,before.counts.proposedMultiMemberFamilies);assert.ok(after.counts.spokenTokens>before.counts.spokenTokens);
});
test('published lexical audit is reproducible from source data',()=>{
 assert.equal(fs.readFileSync(path.join(__dirname,'../docs/curriculum/VOCABULARY_AUDIT.md'),'utf8'),renderLexicalAudit(run())+'\n');
});
