const {test}=require('node:test');
const assert=require('node:assert/strict');
const crypto=require('node:crypto');
const {renderPreview}=require('../curriculum/review-preview.cjs');
const m=require('../docs/curriculum/curriculum_manifest.json'),b=require('../curriculum/drafts/v1/scripts.json'),v=require('../curriculum/drafts/v1/vocabulary.proposed.json'),c=require('../curriculum/characters.json');
test('standalone HTML embeds valid data with restrictive network policy and executable script hash',()=>{
  const h=renderPreview(m,b,v,c),matches=[...h.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/g)];
  assert.equal(matches.length,2);assert.equal(JSON.parse(matches[0][2]).bundle.contentVersion,b.contentVersion);
  const hash=crypto.createHash('sha256').update(matches[1][2]).digest('base64');assert.ok(h.includes(`'sha256-${hash}'`));assert.ok(h.includes("connect-src 'none'"));
  assert.ok(!h.includes('<script src='));assert.ok(!h.includes('<link '));
});
test('script terminators inside authored content stay data rather than executable markup',()=>{
  const changed=structuredClone(b);changed.scripts[0].segments[0].text+=' </script><script>unsafe()</script>';
  const h=renderPreview(m,changed,v,c),data=JSON.parse(h.match(/<script id="payload" type="application\/json">([\s\S]*?)<\/script>/)[1]);
  assert.ok(data.bundle.scripts[0].segments[0].text.includes('</script><script>'));assert.equal([...h.matchAll(/<script\b/g)].length,2);
});
