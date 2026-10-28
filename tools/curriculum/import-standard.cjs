#!/usr/bin/env node
'use strict';
const root=require('node:path').resolve(__dirname,'../..');process.chdir(root);
const fs=require('fs'),crypto=require('crypto');
const path='curriculum/sources/ngsl-1.01/NGSL-1.01.csv',bytes=fs.readFileSync(path);
const rows=bytes.toString('utf8').replace(/^\uFEFF/,'').trim().split(/\r?\n/).map(line=>line.split(',').map(x=>x.trim().replace(/^"|"$/g,'')));
const norm=s=>s.normalize('NFKC').toLowerCase().trim();
const supp=require(process.cwd()+'/curriculum/sources/editorial-supplements.v1.json'),proposed=require(process.cwd()+'/curriculum/drafts/v1/vocabulary.proposed.json');
const words=rows.map((r,i)=>({id:'en.'+norm(r[0]),lemma:norm(r[0]),forms:r.slice(1).map(norm).filter(Boolean),sourceId:'ngsl-1.01',sourceOrder:i+1}));
for(const s of supp)words.push({id:'en.'+s.lemma,lemma:s.lemma,forms:proposed.words.find(w=>w.lemma===s.lemma)?.forms||[],sourceId:'editorial-199',theme:s.theme,rationale:s.reason});
if(rows.length!==2801||supp.length!==199)throw Error('Unexpected source sizes');
const heads=new Set(words.map(w=>w.lemma));if(heads.size!==3000)throw Error('Wrong headword count');
const aliases={airplane:['aeroplane'],yogurt:['yoghurt'],tire:['tyre'],petrol:['gasoline'],windshield:['windscreen']};for(const w of words)w.forms.push(...(aliases[w.lemma]||[]));
const owners=new Map();for(const w of words)for(const form of new Set(w.forms)){if(!owners.has(form))owners.set(form,new Set());owners.get(form).add(w.id);}
const exclusions=[];
for(const w of words){let forms=[];for(const form of new Set(w.forms)){
 let reason=!/^[a-z]+$/.test(form)||form.length<2?'unsupported_or_fragment':heads.has(form)&&form!==w.lemma?'another_headword':owners.get(form).size>1?'ambiguous_between_heads':null;
 if(reason)exclusions.push({id:w.id,form,reason});else if(form!==w.lemma)forms.push(form);
}w.forms=forms;const prop=proposed.words.find(x=>x.id===w.id);if(prop){if(prop.partOfSpeech)w.partOfSpeech=prop.partOfSpeech;w.glossVi=prop.glossVi;}}
const result={schemaVersion:1,version:1,status:'approved_for_beta_baseline',source:'NGSL 1.01 (2801 heads, pinned mirror) + Deep Flow editorial supplement (199 heads)',license:'CC-BY-SA-4.0',countPolicy:'3000 distinct single-word headwords; inflections and spelling aliases add no entries; derived lemmas count separately; chunks excluded',sources:[{id:'ngsl-1.01',authors:['Charles Browne','Brent Culligan','Joseph Phillips'],officialUrl:'https://www.newgeneralservicelist.com/new-general-service-list',mirrorUrl:'https://raw.githubusercontent.com/koba-ninkigumi/ngsl/e78abe02d00988df0f79743e65eac2aa5a43bd03/NGSL-1.01.csv',path,sha256:crypto.createHash('sha256').update(bytes).digest('hex'),headwords:2801,license:'CC-BY-SA-4.0'},{id:'editorial-199',authors:['Deep Flow Language editorial'],path:'curriculum/sources/editorial-supplements.v1.json',sha256:crypto.createHash('sha256').update(fs.readFileSync('curriculum/sources/editorial-supplements.v1.json')).digest('hex'),headwords:199,license:'CC-BY-SA-4.0',note:'Theme-selected, not frequency-ranked.'}],words};
const outputs={'curriculum/vocabulary.standard.json':JSON.stringify(result,null,2)+'\n','curriculum/sources/ngsl-1.01/import-exclusions.json':JSON.stringify({reason:'Form exclusions only, no headword removals; do not infer unambiguous lemmas from rejected forms.',excluded:exclusions},null,2)+'\n'};
if(process.argv.slice(2).some(x=>x!=='--check'))throw Error('Dùng import-standard.cjs [--check]');
for(const [p,content] of Object.entries(outputs)){if(process.argv.includes('--check')){if(fs.readFileSync(p,'utf8')!==content)throw Error('Kết quả nhập chưa đồng bộ: '+p);}else fs.writeFileSync(p,content);}
console.log(words.length,exclusions.length);
