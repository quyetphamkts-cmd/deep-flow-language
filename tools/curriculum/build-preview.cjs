#!/usr/bin/env node
'use strict';
const fs=require('node:fs');
const path=require('node:path');
const {renderPreview}=require('../../curriculum/review-preview.cjs');
const {standardPreview}=require('../../curriculum/standard-preview.cjs');
const root=path.resolve(__dirname,'../..');
function run(args){
  if(args.length)throw new Error('Dùng build-preview.cjs > PATH.html');
  const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
  process.stdout.write(renderPreview(read('docs/curriculum/curriculum_manifest.json'),read('curriculum/drafts/v1/scripts.json'),read('curriculum/drafts/v1/vocabulary.proposed.json'),read('curriculum/characters.json'),standardPreview(read('curriculum/vocabulary.standard.json'),read('curriculum/vocabulary.allocations.json'),read('curriculum/drafts/v1/scripts.json'))));
}
if(require.main===module){try{run(process.argv.slice(2));}catch(e){console.error(e.message);process.exitCode=1;}}
module.exports={run};
