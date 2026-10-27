// Kiểm tra mã web mà không chạy ứng dụng hoặc đọc API key.
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const assert=require('node:assert/strict');
const root=path.join(__dirname,'..');
const html=fs.readFileSync(path.join(root,'www/index.html'),'utf8');
const scripts=[...html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script\s*>/gi)];
assert.equal(scripts.length,2);
scripts.forEach((match,i)=>new vm.Script(match[1],{filename:`inline-${i+1}.js`}));
const manifest=JSON.parse(fs.readFileSync(path.join(root,'update/version.json'),'utf8'));
assert.equal(html.match(/const APP_VER='([^']+)'/)[1],manifest.ver);
const changes=JSON.parse(html.match(/const CHANGELOG=(\[[^\n]+\]);/)[1]);
assert.deepEqual(changes[0],manifest.changelog[0]);
console.log(`OK: 2 script hợp lệ, phiên bản và changelog khớp (${manifest.ver})`);
