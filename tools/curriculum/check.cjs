#!/usr/bin/env node
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const {validateManifest,auditCurriculum} = require('../../curriculum/engine.cjs');
const {preview} = require('../../curriculum/pipeline.cjs');
const root = path.resolve(__dirname,'../..');
const read = p => JSON.parse(fs.readFileSync(p,'utf8'));
function run(args) {
  const allowed = ['--manifest','--vocabulary','--scripts','--preview','--require-vocabulary-ready'];
  const options = {};
  for (let i=0;i<args.length;i++) {
    if (!allowed.includes(args[i])) throw new Error(`Tùy chọn không hỗ trợ: ${args[i]}`);
    const k = args[i];
    if (['--preview','--require-vocabulary-ready'].includes(k)) options[k]=true;
    else { if (!args[i+1] || args[i+1].startsWith('--')) throw new Error(`Thiếu đường dẫn sau ${k}`); options[k]=args[++i]; }
  }
  const manifest = read(options['--manifest'] || path.join(root,'docs/curriculum/curriculum_manifest.json'));
  const errors = validateManifest(manifest);
  if (errors.length) throw new Error(errors.join('\n'));
  const catalog = read(options['--vocabulary'] || path.join(root,'curriculum/vocabulary.pending.json'));
  const bundle = read(options['--scripts'] || path.join(root,'curriculum/scripts.placeholder.json'));
  if (!bundle || bundle.schemaVersion!==1 || !Array.isArray(bundle.scripts)) throw new Error('Gói scripts phải có schemaVersion:1 và scripts[]');
  const report = auditCurriculum(manifest,catalog,bundle.scripts);
  console.log(JSON.stringify(options['--preview'] ? preview(manifest,catalog,bundle.scripts) : {manifestValid:true,coreCount:12,expansionCount:60,...report},null,2));
  return report.errors.length ? 1 : options['--require-vocabulary-ready'] && !report.vocabularyReady ? 2 : 0;
}
if (require.main === module) {
  try { process.exitCode=run(process.argv.slice(2)); } catch(e) { console.error(e.message); process.exitCode=1; }
}
module.exports = {run};
