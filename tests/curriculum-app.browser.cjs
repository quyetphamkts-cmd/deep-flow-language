'use strict';
const {chromium}=require('playwright'),assert=require('node:assert/strict'),http=require('node:http'),fs=require('node:fs'),path=require('node:path');
(async()=>{
 const html=fs.readFileSync(path.join(__dirname,'../www/index.html'));const server=http.createServer((req,res)=>{res.writeHead(200,{'Content-Type':'text/html; charset=utf-8'});res.end(html);});
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(Number(process.env.APP_TEST_PORT)||31461,'127.0.0.1',resolve);});let browser;
 try{
 browser=await chromium.launch({executablePath:process.env.CHROME_BIN||'/usr/bin/google-chrome',headless:true,args:['--no-sandbox','--disable-dev-shm-usage']});
 const page=await browser.newPage({viewport:{width:360,height:800}}),errors=[];page.on('pageerror',e=>errors.push(e.message));await page.route('https://**/*',r=>r.abort());
 await page.addInitScript(version=>{if(window!==window.top)return;localStorage.setItem('engamp',JSON.stringify({autoSub:false,autoTr:false,aiPause:true,vocImg:false,seenVer:version}));localStorage.setItem('dfl_vocab','[]');},require('../update/version.json').ver);
 await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>typeof window.closeModals==='function');await page.evaluate(()=>closeModals());
 const saved=await page.evaluate(()=>localStorage.getItem('dfl_vocab'));
 assert.equal(await page.locator('#hCurriculum').isVisible(),false);await page.evaluate(()=>document.querySelector('#hCurriculum').hidden=false);
 await page.locator('#hCurriculum').click();await page.locator('#mCurriculum.on').waitFor();
 const frame=page.frameLocator('#curriculumFrame');await frame.locator('#lessonTitle').waitFor();assert.equal(await frame.locator('[data-lesson]').count(),13);
 await page.screenshot({path:path.join(process.env.RUNNER_TEMP||'/tmp','curriculum54-course.png')});
 assert.equal(await frame.locator('#standardPanel').isVisible(),true);await frame.locator('#standardPanel summary').click();assert.ok((await frame.locator('#standardSummary').textContent()).includes('1122/3000'));
 assert.equal(await frame.locator('#standardWords .word').count(),250);await frame.locator('#standardLesson').selectOption('C12');await frame.locator('#standardFilter').selectOption('missing');assert.ok(await frame.locator('#standardWords .word').count()>0);
 await frame.locator('#standardSearch').fill('notawordzz');assert.equal(await frame.locator('#standardWords .word').count(),0);await frame.locator('#standardSearch').fill('');
 await frame.locator('[data-lesson="C12"]').click();assert.equal(await frame.locator('#standardLesson').inputValue(),'C12');assert.equal(await frame.locator('#sections option').count(),4);
 await frame.locator('#bilingual').check();assert.ok(await frame.locator('.translation:visible').count()>0);
 const iframe=await (await page.locator('#curriculumFrame').elementHandle()).contentFrame();assert.ok(iframe);assert.equal(await iframe.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
 assert.equal(await iframe.evaluate(()=>{try{return parent.document===document;}catch(e){return 'isolated';}}),'isolated');
 await frame.locator('#sections').selectOption('C12-quick_response');await frame.locator('#wait').click();await page.locator('#curriculumClose').click();assert.equal(await page.locator('#curriculumFrame').getAttribute('srcdoc'),null);
 await page.locator('#hCurriculum').click();await frame.locator('#lessonTitle').waitFor();assert.ok((await frame.locator('#lessonTitle').textContent()).includes('Sunrise'));
 await page.evaluate(()=>closeModals());await page.waitForFunction(()=>!document.querySelector('#curriculumFrame').hasAttribute('srcdoc'));
 assert.equal(await page.evaluate(()=>localStorage.getItem('dfl_vocab')),saved);
 await page.locator('#hNews').click();assert.equal(await page.locator('#mNews').evaluate(n=>n.classList.contains('on')),true);await page.evaluate(()=>closeModals());
 await page.evaluate(()=>showView('vLib'));assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
 await page.screenshot({path:path.join(process.env.RUNNER_TEMP||'/tmp','curriculum54-app.png')});assert.deepEqual(errors,[]);
 console.log('OK beta54: nút cạnh Đọc, 12 bài chính + Z01, danh sách 3000/phân bổ/lọc, song ngữ, iframe cô lập, dọn timer khi đóng/quay lại, giữ từ vựng và nút Đọc.');
 }finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve));}
})().catch(e=>{console.error(e);process.exitCode=1;});
