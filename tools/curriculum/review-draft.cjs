#!/usr/bin/env node
'use strict';
const fs=require('node:fs');
const path=require('node:path');
const {reviewDraft,renderDraft}=require('../../curriculum/draft-review.cjs');
const root=path.resolve(__dirname,'../..');
function run(args){
  let markdown=false,lessonIds;
  for(let i=0;i<args.length;i++){
    if(args[i]==='--markdown')markdown=true;
    else if(args[i]==='--lessons' && args[i+1] && !args[i+1].startsWith('--'))lessonIds=args[++i].split(',');
    else throw new Error('Dùng review-draft.cjs [--markdown [--lessons C01,Z01]]');
  }
  if(lessonIds&&!markdown)throw new Error('--lessons cần --markdown');
  const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
  const m=read('docs/curriculum/curriculum_manifest.json'),b=read('curriculum/drafts/v1/scripts.json'),v=read('curriculum/drafts/v1/vocabulary.proposed.json');
  console.log(markdown?renderDraft(m,b,v,{lessonIds}):JSON.stringify(reviewDraft(m,b,v),null,2));
}
if(require.main===module){try{run(process.argv.slice(2));}catch(e){console.error(e.message);process.exitCode=1;}}
module.exports={run};
