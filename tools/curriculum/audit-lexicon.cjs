#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path');
const {auditLexicon,renderLexicalAudit}=require('../../curriculum/lexical-audit.cjs');
const root=path.resolve(__dirname,'../..');
try{
 const args=process.argv.slice(2);if(args.some(x=>!['--markdown','--require-ready'].includes(x)))throw new Error('Dùng audit-lexicon.cjs [--markdown] [--require-ready]');
 const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
 const r=auditLexicon(read('docs/curriculum/curriculum_manifest.json'),read('curriculum/drafts/v1/scripts.json'),read('curriculum/drafts/v1/vocabulary.proposed.json'),read('curriculum/drafts/v1/word-families.proposed.json'));
 console.log(args.includes('--markdown')?renderLexicalAudit(r):JSON.stringify(r,null,2));
 if(args.includes('--require-ready')&&!r.ready)process.exitCode=2;
}catch(e){console.error(e.message);process.exitCode=1;}
