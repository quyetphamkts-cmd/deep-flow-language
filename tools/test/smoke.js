// Kiểm tra nhanh trước mỗi bản cập nhật — chạy như người dùng thật trên màn hình điện thoại (Pixel 7, cảm ứng).
// Cách chạy: node tools/test/smoke.js <đường-dẫn-http-tới-index.html>  (mạng ngoài bị chặn, máy chủ Google/Diệu Pháp Âm được giả lập)
// Mỗi bước: bấm/chạm thật vào nút, rồi kiểm tra cái người dùng nhìn thấy (phần tử nằm trên cùng, không bị che) + không có lỗi JS.
const {chromium,devices}=require('playwright');
const URL0=process.argv[2];if(!URL0){console.error('thiếu URL');process.exit(2);}
const fails=[],ok=[];const check=(name,cond,info)=>{(cond?ok:fails).push(name+(cond?'':' → '+(info||'')));};
(async()=>{const b=await chromium.launch();const ctx=await b.newContext({...devices['Pixel 7']});const p=await ctx.newPage();const errs=[];
  p.on('pageerror',e=>errs.push(e.message));
  const wav=Buffer.alloc(44+32000*4);/* 4 giây im lặng */wav.write('RIFF',0);wav.writeUInt32LE(36+32000*4,4);wav.write('WAVEfmt ',8);wav.writeUInt32LE(16,16);wav.writeUInt16LE(1,20);wav.writeUInt16LE(1,22);wav.writeUInt32LE(16000,24);wav.writeUInt32LE(32000,28);wav.writeUInt16LE(2,32);wav.writeUInt16LE(16,34);wav.write('data',36);wav.writeUInt32LE(32000*4,40);
  const mkWav=sec=>{const n=Math.round(16000*sec)*2,w=Buffer.alloc(44+n);w.write('RIFF',0);w.writeUInt32LE(36+n,4);w.write('WAVEfmt ',8);w.writeUInt32LE(16,16);w.writeUInt16LE(1,20);w.writeUInt16LE(1,22);w.writeUInt32LE(16000,24);w.writeUInt32LE(32000,28);w.writeUInt16LE(2,32);w.writeUInt16LE(16,34);w.write('data',36);w.writeUInt32LE(n,40);for(let i=0;i<n/2;i++)w.writeInt16LE(Math.round(3000*Math.sin(i/8)),44+i*2);return w;};const wavS=mkWav(0.6);
  await ctx.route(/^https?:\/\/(?!127\.0\.0\.1)/,r=>{const u=r.request().url();
    if(u.includes('texttospeech.googleapis.com/v1/voices'))return r.fulfill({status:200,contentType:'application/json',body:JSON.stringify({voices:[{name:'vi-VN-Chirp3-HD-Aoede',ssmlGender:'FEMALE'},{name:'vi-VN-Chirp3-HD-Charon',ssmlGender:'MALE'}]})});
    if(u.includes('text:synthesize'))return new Promise(res=>setTimeout(()=>res(r.fulfill({status:200,contentType:'application/json',body:JSON.stringify({audioContent:wavS.toString('base64')})})),400));
    if(u.includes('media.test'))return r.fulfill({status:200,contentType:'audio/wav',body:wav});
    if(u.includes('api.github.com')&&u.includes('korva'))return r.fulfill({status:200,contentType:'application/json',body:JSON.stringify({assets:['gia_bao','khanh_vy','bao_kim'].map(v=>({name:'sample_'+v+'.mp3',browser_download_url:'https://media.test/s_'+v+'.wav',size:1000}))})});
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
  // chỉ nghe bản dịch (giọng Google AI): các câu đọc liền nhau, không ngắt quãng
  {const r=await p.evaluate(async()=>{const srt=[0,1,2,3,4].map(k=>`${k+1}\n00:00:0${k},000 --> 00:00:0${k},900\nSentence ${k}.\n`).join('\n');
      const t={id:'vo1',name:'VO test',album:'VO',size:0,loc:true,subs:{main:srt},trans:Object.fromEntries([0,1,2,3,4].map(k=>['Sentence '+k+'.','Câu số '+k+'.'])),added:Date.now(),lg:effLg()};
      await putMeta0(t);await putAudio('vo1',await getAudio(tracks[0].id));tracks.push(t);await loadTrack(t,false);audio.muted=true;
      S.viEng='gcloud';S.gcKey='AIzaTEST';S.gcVoice='vi-VN-Chirp3-HD-Aoede';GC.off=0;window._voT=[];const A0=window.Audio;window.Audio=function(u){const a=new A0(u);a.muted=true;a.addEventListener('playing',()=>_voT.push(performance.now()));a.addEventListener('ended',()=>_voT.push(-performance.now()));return a;};
      S.viOnly=true;voStart();await new Promise(r=>setTimeout(r,9000));voStop();window.Audio=A0;S.viOnly=false;
      const st=_voT.filter(x=>x>0),en=_voT.filter(x=>x<0).map(x=>-x);const gaps=[];for(let k=0;k+1<st.length;k++)gaps.push(st[k+1]-en[k]);return {n:st.length,gaps};});
    check('chỉ nghe bản dịch: đọc đủ các câu',r.n>=4,'mới đọc '+r.n+' câu');console.log('   khoảng nghỉ giữa câu: '+r.gaps.map(x=>Math.round(x)+'ms').join(', '));check('chỉ nghe bản dịch: không ngắt quãng (khoảng nghỉ < 0,45 s)',r.gaps.length&&Math.max(...r.gaps)<450,'khoảng nghỉ '+r.gaps.map(x=>Math.round(x)+'ms').join(', '));}
  // KorvaTTS (giả lập đúng cách app Android chạy: 1 luồng tạo giọng — mỗi câu 400 ms, phát trên luồng riêng 600 ms)
  await p.evaluate(()=>{const W=[];let busy=Promise.resolve();const cache=new Set();const q=f=>{const r=busy.then(f);busy=r.catch(()=>{});return r;};const sleep=ms=>new Promise(r=>setTimeout(r,ms));
    window._kvLog=[];window.dfKorva={status:async()=>({installed:true,size:4e8}),load:async()=>({ms:10}),stop:async()=>{},addListener:async()=>({remove(){}}),
      prefetch:o=>q(async()=>{if(!cache.has(o.text)){await sleep(400);cache.add(o.text);}_kvLog.push(['pre',o.text,performance.now()]);}),
      speak:o=>q(async()=>{if(!cache.has(o.text)){await sleep(400);cache.add(o.text);}}).then(async()=>{_kvLog.push(['play',o.text,performance.now(),o.voice]);await sleep(600);_kvLog.push(['end',o.text,performance.now()]);})};
    KV.st={installed:true,size:4e8};KV.loading=null;});
  await p.evaluate(()=>{showView('vSet');setTimeout(()=>viPickAll(),0);});await p.waitForTimeout(700);
  if(await p.locator('[data-vp="kv:bao_kim"]').count()){await p.locator('[data-vp="kv:bao_kim"]').scrollIntoViewIfNeeded();await p.tap('[data-vp="kv:bao_kim"]');await p.waitForTimeout(1500);
    const r=await p.evaluate(()=>({sel:S.viEng+':'+S.viVoice,open:document.querySelector('#mSheet').classList.contains('on'),ask:document.querySelector('#mAsk').classList.contains('on'),said:_kvLog.some(x=>x[0]==='play'&&x[3]==='bao_kim')}));
    check('Korva (đã tải): chạm → nghe ngay + chọn, bảng vẫn mở, không hỏi',r.sel==='korva:bao_kim'&&r.open&&!r.ask&&r.said,JSON.stringify(r));}
  await p.evaluate(()=>closeModals());
  {const r=await p.evaluate(async()=>{const t=tracks.find(x=>x.id==='vo1');if(!t)return {n:0,gaps:[]};await loadTrack(t,false);audio.muted=true;_kvLog.length=0;S.viEng='korva';S.viVoice='bao_kim';S.viOnly=true;voStart();await new Promise(r=>setTimeout(r,6500));voStop();S.viOnly=false;
      const pl=_kvLog.filter(x=>x[0]==='play'),en=_kvLog.filter(x=>x[0]==='end');const gaps=[];for(let k=0;k+1<pl.length;k++)gaps.push(pl[k+1][2]-en[k][2]);return {n:pl.length,gaps};});
    console.log('   Korva — khoảng nghỉ giữa câu: '+r.gaps.map(x=>Math.round(x)+'ms').join(', '));
    check('Korva chỉ nghe bản dịch: không ngắt quãng (< 0,45 s)',r.n>=4&&Math.max(...r.gaps)<450,'đọc '+r.n+' câu, khoảng nghỉ '+r.gaps.map(x=>Math.round(x)).join(','));}
  // Korva chưa tải: chạm chỉ nghe mẫu, không bật hộp hỏi
  await p.evaluate(()=>{window.dfKorva.status=async()=>({installed:false});KV.st={installed:false};S.viEng='sys';setTimeout(()=>viPickAll(),0);});await p.waitForTimeout(700);
  if(await p.locator('[data-vp="kv:gia_bao"]').count()){await p.locator('[data-vp="kv:gia_bao"]').scrollIntoViewIfNeeded();await p.tap('[data-vp="kv:gia_bao"]');await p.waitForTimeout(900);
    const r=await p.evaluate(()=>({ask:document.querySelector('#mAsk').classList.contains('on'),open:document.querySelector('#mSheet').classList.contains('on'),dl:!!document.querySelector('[data-vpa="kvdl"]'),pv:!!document.querySelector('[data-vp="kv:gia_bao"].pv')}));
    check('Korva (chưa tải): chạm → nghe mẫu tại chỗ, không hỏi, có nút tải',!r.ask&&r.open&&r.dl&&r.pv,JSON.stringify(r));}
  await p.evaluate(()=>{closeModals();delete window.dfKorva;S.viEng='gcloud';});
  // khoá API còn nguyên sau khi mở lại app / cập nhật, kể cả khi cài đặt bị ghi đè
  await p.reload();await p.waitForTimeout(4000);check('khoá Google còn sau khi mở lại app',await p.evaluate(()=>S.gcKey==='AIzaTEST'));
  await p.evaluate(()=>{const o=JSON.parse(localStorage.getItem('engamp'));delete o.gcKey;localStorage.setItem('engamp',JSON.stringify(o));});await p.reload();await p.waitForTimeout(4000);
  check('khoá Google còn khi cài đặt bị ghi đè',await p.evaluate(()=>S.gcKey==='AIzaTEST'));
  check('khoá Google hiện trong Cài đặt → AI',await p.evaluate(()=>{const r=document.querySelector('.keyrow[data-k="gcKey"] .kval');return !!r&&/AIza/.test(r.textContent);}));
  // tốc độ giọng Việt theo tốc độ trình phát; menu Phụ đề không còn mục tốc độ riêng
  {const r=await p.evaluate(async()=>{setRate(1.25);let sent=null;const f0=window.fetch;window.fetch=function(u,o){if(String(u).includes('text:synthesize'))sent=JSON.parse(o.body).audioConfig.speakingRate;return f0.apply(this,arguments);};
     S.viEng='gcloud';GC.off=0;try{await gcBytes('Câu kiểm tra tốc độ '+Date.now(),undefined);}catch(e){}window.fetch=f0;setRate(1);return {sent,korva:kvOpt('x').speed};});
   check('giọng Việt đọc theo tốc độ trình phát (1.25×)',r.sent===1.25,JSON.stringify(r));}
  // mở app khi đang chọn giọng KorvaTTS: không lỗi khởi động
  {const n0=errs.length;await p.evaluate(()=>{S.viEng='korva';S.viVoice='bao_kim';save();});await p.reload();await p.waitForTimeout(4000);check('mở app khi đang chọn giọng Korva',errs.length===n0,errs.slice(n0).join(' | '));}
  check('không có lỗi JS',errs.length===0,errs.join(' | '));
  console.log('✓ '+ok.length+' bước đạt');if(fails.length){console.log('✗ '+fails.length+' bước LỖI:\n  '+fails.join('\n  '));}
  await b.close();process.exit(fails.length?1:0);})();
