const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const http=require('node:http');
const fs=require('node:fs');
(async()=>{
 const server=http.createServer((req,res)=>{res.setHeader('Content-Type','text/html');res.end(fs.readFileSync(require.resolve('../www/index.html')));});
 await new Promise(r=>server.listen(31465,'127.0.0.1',r));
 const browser=await chromium.launch({executablePath:process.env.CHROME_BIN||'/usr/bin/google-chrome',headless:true,args:['--no-sandbox','--disable-dev-shm-usage']});
 try{
  const page=await browser.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));await page.route('https://**/*',r=>r.abort());
  await page.addInitScript(ver=>{if(window!==window.top)return;localStorage.setItem('engamp',JSON.stringify({seenVer:ver,autoSub:false,autoTr:false,aiPause:true,vocImg:false}));},require('../update/version.json').ver);
  await page.goto('http://127.0.0.1:31465');await page.waitForTimeout(600);
  await page.evaluate(()=>{Math.random=()=>.99999;closeModals();showView('vVoc');S.fcFlow='manual';window.read58=[];window.stop58=0;window.web58=[];window.dfSpeak=(w,l,active)=>{if(active())read58.push(w);return Promise.resolve();};window.dfSpeakStop=()=>{stop58++;};speechSynthesis.speak=u=>web58.push(u.text);window.cards58=['first','second'].map(word=>({id:word,word,lang:'en',ipa:'/test/',ex:[{s:'One.'},{s:'Two.'}],srs:{due:0,ivl:0,reps:0}}));startReview(cards58);});
  await page.waitForFunction(()=>read58.length===1);assert.deepEqual(await page.evaluate(()=>read58),['first']);
  await page.locator('#fcard').click();assert.equal(await page.evaluate(()=>rv.flipped),true);assert.deepEqual(await page.evaluate(()=>read58),['first']);
  await page.locator('#fcSay').click();assert.deepEqual(await page.evaluate(()=>read58),['first','first']);
  await page.evaluate(()=>moveCard(1));await page.waitForFunction(()=>read58.length===3);assert.equal(await page.evaluate(()=>read58.at(-1)),'second');assert.equal(await page.evaluate(()=>rv.flipped),false);
  // Chuyển tự động vẫn chỉ đọc thẻ mới, không lật hoặc chấm mức nhớ.
  await page.locator('#cardFlow51').click();await page.waitForTimeout(3150);assert.equal(await page.evaluate(()=>read58.at(-1)),'first');assert.equal(await page.evaluate(()=>rv.flipped),false);assert.equal(await page.evaluate(()=>rv.answers),0);await page.locator('#cardFlow51').click();
  // Lỗi native phản hồi sau khi chuyển thẻ không được phát giọng cũ dự phòng.
  await page.evaluate(()=>{window.dfSpeak=w=>new Promise((resolve,reject)=>window.reject58=reject);cardSpeak58();});
  await page.evaluate(()=>{window.oldReject58=reject58;window.dfSpeak=(w,l,active)=>{if(active())read58.push(w);return Promise.resolve();};moveCard(1);oldReject58(new Error('late'));});await page.waitForTimeout(80);assert.deepEqual(await page.evaluate(()=>web58),[]);
  // Audio từ điển được theo dõi; đổi thẻ phải dừng Audio trước và hủy fallback muộn.
  await page.evaluate(()=>{window.RealAudio58=Audio;window.audios58=[];window.Audio=class {constructor(src){this.src=src;this.paused=false;audios58.push(this);}play(){return new Promise((res,rej)=>this.reject=rej);}pause(){this.paused=true;}removeAttribute(){this.src='';}load(){}};cards58[rv.i].au='https://example.test/word.mp3';cardSpeak58();});
  await page.evaluate(()=>{window.before58=read58.length;moveCard(1);audios58[0].reject(new Error('late audio'));});await page.waitForTimeout(80);assert.equal(await page.evaluate(()=>audios58[0].paused),true);assert.equal(await page.evaluate(()=>read58.length),await page.evaluate(()=>before58+1));
  // Từ chối autoplay: không tự gọi thêm nguồn, người học có thể bấm Nghe lại.
  await page.evaluate(()=>{cards58[rv.i].au='https://example.test/blocked.mp3';cardSpeak58();window.blockedCount58=read58.length;const e=new Error('blocked');e.name='NotAllowedError';audios58.at(-1).reject(e);});await page.waitForTimeout(80);assert.equal(await page.evaluate(()=>read58.length),await page.evaluate(()=>blockedCount58));assert.match(await page.locator('#fcSay').getAttribute('title'),/Chạm Nghe lại/);
  // Không phát câu gốc muộn sau khi đã rời phiên ôn.
  await page.evaluate(async()=>{window.Audio=RealAudio58;await putAudio('pending',new Blob([wav16(new Float32Array(32000))],{type:'audio/wav'}));await playClip('pending',0,1);const late=clip.onloadedmetadata;endReview();if(late)late();});await page.waitForTimeout(80);assert.equal(await page.evaluate(()=>clip.paused),true);
  const n=await page.evaluate(()=>read58.length);await page.waitForTimeout(200);assert.equal(await page.evaluate(()=>read58.length),n);assert.ok(await page.evaluate(()=>stop58>0));
  // Thẻ đã hết hạn không phát lại từ cũ; rời trang cũng dừng phát.
  await page.evaluate(()=>{cards58.forEach(v=>{delete v.au;v.srs.due=Date.now()+3600000;});startReview(cards58);});await page.waitForTimeout(80);assert.equal(await page.evaluate(()=>read58.length),n);assert.equal(await page.locator('#fcSummary').isVisible(),true);await page.evaluate(()=>showView('vPlay'));assert.equal(await page.locator('#vocRev').isVisible(),false);
  await page.evaluate(()=>{showView('vVoc');const v={id:'known65',word:'remember',lang:'en',tmp:true,srs:{due:0,ivl:0,reps:0}};startReview([v]);});assert.equal(await page.locator('#fcKnown65').isVisible(),true);await page.locator('#fcKnown65').click();assert.equal(await page.locator('#fcSummary').isVisible(),true);assert.equal(await page.evaluate(()=>vocab.some(v=>v.id==='known65'&&v.srs.knownAt65)),true);assert.equal(await page.evaluate(()=>fcDueQueue().some(v=>v.id==='known65')),false);await page.locator('#fcUndo').click();assert.equal(await page.locator('#fcWord').textContent(),'remember');assert.equal(await page.evaluate(()=>vocab.some(v=>v.id==='known65')),false);
  // Android-style touch gestures on both scrollable faces; translations follow selected target.
  const mobile=await browser.newPage({viewport:{width:360,height:800},isMobile:true,hasTouch:true});mobile.on('pageerror',e=>errors.push(e.message));await mobile.route('https://**/*',r=>r.abort());await mobile.route('**/translate.googleapis.com/**',r=>{const q=new URL(r.request().url()).searchParams;assert.equal(q.get('tl'),'vi');return r.fulfill({json:[[[q.get('q')==='first'?'đầu tiên':q.get('q')==='second'?'thứ hai':'Câu tiếng Việt',null,null]] ]});});
  await mobile.goto('http://127.0.0.1:31465');await mobile.waitForFunction(()=>typeof cardTranslations66==='function');await mobile.evaluate(()=>{Math.random=()=>.99999;closeModals();S.autoSub=false;S.vocImg=false;S.trLang='vi';S.fcFlow='manual';window.dfSpeak=()=>Promise.resolve();showView('vVoc');startReview(['first','second'].map(word=>({id:word,word,lang:'en',mean:'中文翻译',ctx:{text:'A sentence.',tr:'中文句子'},ex:[{s:'Example.',t:'中文示例'}],srs:{due:0,ivl:0,reps:0}})));});
  await mobile.waitForFunction(()=>document.querySelector('#fcMean').textContent==='đầu tiên');assert.equal(await mobile.locator('#fcTr').textContent(),'Câu tiếng Việt');
  const cdp=await mobile.context().newCDPSession(mobile);const swipe=async dx=>{const box=await mobile.locator('#fcard').boundingBox();const x=box.x+box.width/2,y=box.y+box.height/2;await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x,y}]});for(let i=1;i<=6;i++)await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:x+dx*i/6,y:y+i}]});await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});};
  await swipe(-100);assert.equal(await mobile.locator('#fcWord').textContent(),'second');assert.equal(await mobile.evaluate(()=>rv.flipped),false);await mobile.locator('#fcard').tap();assert.equal(await mobile.evaluate(()=>rv.flipped),true);assert.doesNotMatch(await mobile.locator('#fcBack').textContent(),/[\p{Script=Han}]/u);await swipe(100);assert.equal(await mobile.locator('#fcWord').textContent(),'first');assert.equal(await mobile.evaluate(()=>rv.flipped),false);assert.equal(await mobile.evaluate(()=>rv.answers),0);await mobile.close();
  assert.deepEqual(errors,[]);console.log('OK: auto/replay word audio, no flip/grade, stop on change/exit, stale native/audio/clip cancellation, blocked autoplay.');
 }finally{await browser.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
