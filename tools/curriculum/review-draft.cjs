#!/usr/bin/env node
'use strict';
const fs=require('node:fs');
const path=require('node:path');
const {reviewDraft,renderDraft}=require('../../curriculum/draft-review.cjs');
const root=path.resolve(__dirname,'../..');
function run(args){
  if(args.length>1 || args.length===1 && args[0]!=='--markdown')throw new Error('Dùng review-draft.cjs [--markdown]');
  const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
  const m=read('docs/curriculum/curriculum_manifest.json'),b=read('curriculum/drafts/v1/scripts.json'),v=read('curriculum/drafts/v1/vocabulary.proposed.json');
  console.log(args[0]==='--markdown'?renderDraft(m,b,v):JSON.stringify(reviewDraft(m,b,v),null,2));
}
if(require.main===module){try{run(process.argv.slice(2));}catch(e){console.error(e.message);process.exitCode=1;}}
module.exports={run};
