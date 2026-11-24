// Kiểm tra nhanh trước mỗi bản cập nhật — chạy như người dùng thật trên màn hình điện thoại (Pixel 7, cảm ứng).
// Cách chạy: node tools/test/smoke.js <đường-dẫn-http-tới-index.html>  (mạng ngoài bị chặn, máy chủ Google/Diệu Pháp Âm được giả lập)
// Mỗi bước: bấm/chạm thật vào nút, rồi kiểm tra cái người dùng nhìn thấy (phần tử nằm trên cùng, không bị che) + không có lỗi JS.
const {chromium,devices}=require('playwright');
const URL0=process.argv[2];if(!URL0){console.error('thiếu URL');process.exit(2);}
const fails=[],ok=[];const check=(name,cond,info)=>{(cond?ok:fails).push(name+(cond?'':' → '+(info||'')));};
(async()=>{const b=await chromium.launch();const ctx=await b.newContext({...devices['Pixel 7']});const p=await ctx.newPage();const errs=[];
  p.on('pageerror',e=>errs.push(e.message));
  const wav=Buffer.alloc(44+32000*4);wav.write('RIFF',0);wav.writeUInt32LE(36+32000*4,4);wav.write('WAVEfmt ',8);wav.writeUInt32LE(16,16);wav.writeUInt16LE(1,20);wav.writeUInt16LE(1,22);wav.writeUInt32LE(16000,24);wav.writeUInt32LE(32000,28);wav.writeUInt16LE(2,32);wav.writeUInt16LE(16,34);wav.write('data',36);wav.writeUInt32LE(32000*4,40);
  await ctx.route(/^https?:\/\/(?!127\.0\.0\.1)/,r=>{const u=r.request().url();
    if(u.includes('texttospeech.googleapis.com/v1/voices'))return r.fulfill({status:200,contentType:'application/json',body:JSON.stringify({voices:[{name:'vi-VN-Chirp3-HD-Aoede',ssmlGender:'FEMALE'},{name:'vi-VN-Chirp3-HD-Charon',ssmlGender:'MALE'}]})});
    if(u.includes('text:synthesize'))return r.fulfill({status:200,contentType:'application/json',body:JSON.stringify({audioContent:wav.toString('base64')})});
    if(u.includes('media.test'))return r.fulfill({status:200,contentType:'audio/wav',body:wav});
    return r.abort();});
  // phần tử ở giữa có thật sự hiện trên cùng (không bị bảng khác che)?
  const onTop=sel=>p.evaluate(s=>{const e=document.querySelector(s);if(!e||!e.offsetParent)return 'không hiện';const r=e.getBoundingClientRect();const t=document.elementFromPoint(r.left+r.width/2,r.top+Math.min(r.height/2,20));return t&&(t===e||e.contains(t))?true:'bị che bởi '+(t?(t.id||t.className||t.tagName):'?');},sel);
  const tapSel=async sel=>{try{await p.locator(sel).first().scrollIntoViewIfNeeded({timeout:3000}).catch(()=>{});await p.tap(sel,{timeout:5000});return true;}catch(e){check('chạm '+sel,false,'không chạm được (không hiện / bị che)');return false;}};
  const cdp=await ctx.newCDPSession(p);const hold=async sel=>{const bb=await p.locator(sel).first().boundingBox();const x=bb.x+60,y=bb.y+bb.height/2;await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x,y}]});await p.waitForTimeout(800);await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});await p.waitForTimeout(500);};
  await p.goto(URL0);await p.waitForTimeout(3500);check('mở app',errs.length===0,errs.join(' | '));
  // dữ liệu mẫu: 2 bài tiếng Anh + 2 bài Pháp thoại
  await p.evaluate(async()=>{const buf=await (await fetch('https://media.test/a.wav')).arrayBuffer();S.lists=[...(S.lists||[]),{id:'tK',name:'Khoá thử',lg:'dh'}];
    for(const [id,lg,al] of [['e1','en','Eng A'],['e2','en','Eng A'],['d1','dh','Kinh thử'],['d2','dh','Kinh thử']]){const t={id,name:'Bai '+id,album:al,size:0,loc:true,subs:{},added:Date.now(),lg};await putMeta0(t);await putAudio(id,new Blob([buf],{type:'audio/wav'}));(lg===effLg()?tracks:HIDT).push(t);}
    setUnitList('Kinh thử','tK');save();renderLib();});
  // các tab
  for(const [sel,name] of [['nav button[data-dh]','tab Pháp thoại'],['nav button[data-v="vLib"]','tab Khoá học'],['nav button[data-v="vPlay"]','tab Nghe'],['nav button[data-v="vSet"]','tab Cài đặt']]){
    if(await p.locator(sel).count()){await tapSel(sel);await p.waitForTimeout(500);check(name,errs.length===0,errs.slice(-1).join(''));}}
  // Cài đặt → bảng giọng → nhập khoá (ô nhập phải hiện trên cùng) → chọn giọng
  await p.evaluate(()=>{delete S.gcKey;showView('vSet');});await p.waitForTimeout(300);
  {const tb=p.locator('button',{hasText:'Tài khoản'}).first();if(await tb.count())await tb.tap().catch(()=>{});await p.waitForTimeout(400);}
  if(await p.locator('#viRow').count()){await tapSel('#viRow');await p.waitForTimeout(600);check('mở bảng giọng',await p.evaluate(()=>document.querySelector('#mSheet').classList.contains('on')));
    await tapSel('[data-vpa="gckey"]');await p.waitForTimeout(400);const t=await onTop('#askIn');check('ô nhập khoá hiện trên cùng',t===true,t);
    if(t===true){await p.fill('#askIn','AIzaTEST');await tapSel('#askOk');await p.waitForTimeout(1000);
      check('lưu khoá + hiện giọng Google',await p.evaluate(()=>S.gcKey==='AIzaTEST'&&document.querySelector('#sheetBox').innerText.includes('Aoede')));
      // cuộn xuống rồi chạm 1 giọng ở dưới → danh sách phải giữ nguyên chỗ, không nhảy lên đầu
      await p.evaluate(()=>{const sc=document.querySelector('#sheetBox .vpScroll');if(sc)sc.scrollTop=sc.scrollHeight;});await p.waitForTimeout(200);
      const top0=await p.evaluate(()=>{const sc=document.querySelector('#sheetBox .vpScroll');return sc?sc.scrollTop:-1;});
      const lastV=await p.evaluate(()=>{const r=[...document.querySelectorAll('#sheetBox [data-vp^="kv:"]')].pop();return r?r.dataset.vp:null;});
      if(lastV){await p.tap('[data-vp="'+lastV+'"]',{timeout:5000}).catch(()=>{});await p.waitForTimeout(700);const top1=await p.evaluate(()=>{const sc=document.querySelector('#sheetBox .vpScroll');return sc?sc.scrollTop:-1;});
        check('chạm nghe thử: danh sách giữ nguyên chỗ',top0>0&&Math.abs(top1-top0)<5,'trước '+top0+' sau '+top1);
        await p.evaluate(()=>{document.querySelector('#mAsk').classList.remove('on');});}
      await tapSel('[data-vp="gc:vi-VN-Chirp3-HD-Aoede"]');await p.waitForTimeout(800);check('chạm giọng: chọn + bảng vẫn mở',await p.evaluate(()=>S.gcVoice==='vi-VN-Chirp3-HD-Aoede'&&document.querySelector('#mSheet').classList.contains('on')));}
    await p.evaluate(()=>closeModals());}
  // mọi hộp hỏi phải nằm trên bảng đang mở
  await p.evaluate(()=>{openModal('mSheet');setTimeout(()=>askConfirm('thử','OK'),0);});await p.waitForTimeout(300);{const t=await onTop('#askOk');check('hộp hỏi trên bảng',t===true,t);}await p.evaluate(()=>{document.querySelector('#mAsk').classList.remove('on');closeModals();});
  // Pháp thoại: nhấn giữ bài → menu có Chọn → ☰ hiện
  await p.evaluate(()=>{if(!DHM)dhMode(true);});await p.waitForTimeout(600);await p.evaluate(()=>{S.secCol={};S.albCol={};renderLib();});await p.waitForTimeout(300);
  if(await p.locator('#libList .trk').count()){await hold('#libList .trk');const has=await p.locator('#sheetBox [data-a="__pick"]').count();check('nhấn giữ bài → có ☑️ Chọn',has>0);
    if(has){await tapSel('#sheetBox [data-a="__pick"]');await p.waitForTimeout(500);check('Chọn → hiện ☰',await p.evaluate(()=>selMode&&[...document.querySelectorAll('#libList .hdl')].some(x=>x.offsetParent)));await p.evaluate(()=>{finishLibraryEdit53();if(selMode)exitSel();});}}
  // phát + tua bằng thanh nhỏ
  await p.evaluate(async()=>{await loadTrack(tracks[0],false);audio.muted=true;showView('vLib');updMini();});await p.waitForTimeout(800);
  if(await p.locator('#miniWave').count()){const bb=await p.locator('#miniWave').boundingBox();if(bb){await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:bb.x+10,y:bb.y+bb.height/2}]});for(let k=1;k<=6;k++){await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:bb.x+10+k*bb.width/12,y:bb.y+bb.height/2}]});await p.waitForTimeout(20);}
    check('kéo tua hiện thời gian',await p.evaluate(()=>{const t=document.querySelector('#mwTip');return t&&t.style.display==='block';}));await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});await p.waitForTimeout(400);
    check('thả tay → tua tới giữa bài',await p.evaluate(()=>audio.currentTime>0.3));}}
  check('không có lỗi JS',errs.length===0,errs.join(' | '));
  console.log('✓ '+ok.length+' bước đạt');if(fails.length){console.log('✗ '+fails.length+' bước LỖI:\n  '+fails.join('\n  '));}
  await b.close();process.exit(fails.length?1:0);})();
