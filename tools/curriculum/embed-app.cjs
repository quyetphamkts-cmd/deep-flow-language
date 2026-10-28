#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path');const root=path.resolve(__dirname,'../..');
const {renderPreview}=require('../../curriculum/review-preview.cjs'),{standardPreview}=require('../../curriculum/standard-preview.cjs');
const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
const bundle=read('curriculum/drafts/v1/scripts.json');
const preview=renderPreview(read('docs/curriculum/curriculum_manifest.json'),bundle,read('curriculum/drafts/v1/vocabulary.proposed.json'),read('curriculum/characters.json'),standardPreview(read('curriculum/vocabulary.standard.json'),read('curriculum/vocabulary.allocations.json'),bundle));
const wire=JSON.stringify(preview).replace(/</g,'\\u003c').replace(/\u2028/g,'\\u2028').replace(/\u2029/g,'\\u2029');
const generated=`// BEGIN CURRICULUM BETA EMBED\n(()=>{\n const preview=${wire};\n const modal=document.getElementById('mCurriculum'),frame=document.getElementById('curriculumFrame'),button=document.getElementById('hCurriculum');\n button.onclick=()=>{closeModals();frame.srcdoc=preview;openModal('mCurriculum');document.getElementById('curriculumClose').focus();};\n document.getElementById('curriculumClose').onclick=()=>modal.classList.remove('on');\n new MutationObserver(()=>{if(!modal.classList.contains('on')&&frame.hasAttribute('srcdoc')){frame.removeAttribute('srcdoc');button.focus();}}).observe(modal,{attributes:true,attributeFilter:['class']});\n})();\n// END CURRICULUM BETA EMBED`;
const app=path.join(root,'www/index.html');let html=fs.readFileSync(app,'utf8');
if(!html.includes('id="hCurriculum"')||!html.includes('id="mCurriculum"'))throw Error('Thiếu nút hoặc hộp giáo trình');
if(html.includes('// BEGIN CURRICULUM BETA EMBED'))html=html.replace(/\/\/ BEGIN CURRICULUM BETA EMBED[\s\S]*?\/\/ END CURRICULUM BETA EMBED/,()=>generated);
else html=html.replace('</script>',()=>generated+'\n</script>');
if(process.argv.includes('--check')){if(html!==fs.readFileSync(app,'utf8'))throw Error('Bản giáo trình nhúng chưa đồng bộ');console.log('OK: giáo trình nhúng đồng bộ');}else fs.writeFileSync(app,html);
