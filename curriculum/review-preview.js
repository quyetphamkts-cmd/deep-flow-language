/* Bản đọc thử độc lập: không mạng, không lưu tiến độ, không tạo audio. */
(()=>{
  'use strict';
  const data=JSON.parse(document.getElementById('payload').textContent), $=id=>document.getElementById(id);
  const titles={main_story:'Truyện chính',word_discovery:'Khám phá từ',quick_response:'Phản xạ hỏi đáp',time_shift:'Đổi thời gian & góc nhìn',daily_adventure:'Câu chuyện phiêu lưu'};
  const byId=new Map(data.bundle.scripts.map(s=>[s.id,s]));
  let lesson='C01',script=byId.get('C01-main_story'),qi=0,remaining=0,mode='idle',deadline=0,timer=null,generation=0,revealed=false,sourceOpen=false;
  function node(tag,text,cls){const n=document.createElement(tag);if(text!==undefined)n.textContent=text;if(cls)n.className=cls;return n;}
  const questions=()=>script.segments.filter(s=>s.type==='question');
  const current=()=>questions()[qi];
  function stop(){generation++;if(timer!==null)clearTimeout(timer);timer=null;}
  function syncTranslations(){document.querySelectorAll('.translation').forEach(n=>n.hidden=!$('bilingual').checked);}
  function paragraph(p){const box=node('section',undefined,'paragraph');box.append(node('p',p.text,'english'));if(p.translationVi)box.append(node('p',p.translationVi,'translation'));return box;}
  function renderFamily(){
    $('family').hidden=data.manifest.lessons.find(l=>l.id===lesson).type!=='core';
    const labels={david:'bố', 'david-wife':'mẹ','david-son':'con trai lớn','david-older-daughter':'con gái lớn','david-younger-daughter':'con gái út'};
    const roles={architect:'Kiến trúc sư',accountant:'Kế toán'};
    $('members').replaceChildren(...data.characters.family.members.map(m=>{
      const box=node('div',undefined,'member');box.append(node('strong',`${m.name} · ${labels[m.id]}`),node('span',m.occupation?roles[m.occupation]:m.schoolGrade?`Học lớp ${m.schoolGrade}`:`${m.ageYears} tuổi`));return box;
    }));
  }
  function renderSource(){
    const box=$('source');box.hidden=!sourceOpen;box.replaceChildren();if(!sourceOpen)return;
    box.append(node('h3','Câu trong bài'));
    for(const ref of current().sourceRefs){const p=byId.get(ref.scriptId).segments.find(p=>p.id===ref.segmentId);box.append(paragraph(p));}
    syncTranslations();
  }
  function renderTimer(){
    $('wait').disabled=revealed;
    $('wait').textContent=mode==='running'?'Tạm dừng chờ':mode==='paused'?'Tiếp tục chờ':`Chờ ${current().answerPauseMs/1000} giây`;
    $('status').textContent=mode==='running'?`Còn ${Math.ceil(remaining/1000)} giây để trả lời`:mode==='paused'?`Đã tạm dừng · còn ${Math.ceil(remaining/1000)} giây`:revealed?'Đã mở câu trả lời mẫu. Cách diễn đạt tương đương cũng được.':'Hãy thử trả lời thành tiếng trước khi xem mẫu.';
  }
  function reveal(){stop();revealed=true;remaining=0;mode='answered';$('answerBox').hidden=false;$('reveal').disabled=true;renderTimer();}
  function pause(){if(mode!=='running')return;remaining=Math.max(0,deadline-performance.now());stop();mode='paused';renderTimer();}
  function wait(){
    if(mode==='running'){pause();return;}
    if(revealed || !current())return;
    stop();const token=generation;deadline=performance.now()+remaining;mode='running';
    function tick(){
      if(token!==generation)return;
      remaining=Math.max(0,deadline-performance.now());if(!remaining){reveal();return;}
      renderTimer();timer=setTimeout(tick,100);
    }
    tick();
  }
  function renderQuestion(){
    stop();const qs=questions(),q=qs[qi];mode='idle';remaining=q.answerPauseMs;revealed=false;sourceOpen=false;
    $('counter').textContent=`Câu ${qi+1} / ${qs.length}`;$('question').textContent=q.text;
    $('answerText').textContent=script.segments.find(p=>p.id===q.answerId).text;
    $('answerBox').hidden=true;$('reveal').disabled=false;$('previous').disabled=qi===0;$('next').disabled=qi===qs.length-1;
    renderSource();renderTimer();
  }
  function renderSection(){
    stop();mode='idle';qi=0;
    const practice=script.component==='quick_response';$('practice').hidden=!practice;$('story').hidden=practice;
    $('sectionTitle').textContent=titles[script.component];
    if(practice)renderQuestion();else $('story').replaceChildren(...script.segments.filter(p=>p.type==='narration').map(paragraph));
    syncTranslations();
  }
  function selectLesson(id){
    lesson=id;const list=data.bundle.scripts.filter(s=>s.lessonId===id);script=list[0];
    document.querySelectorAll('[data-lesson]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.lesson===id)));
    const l=data.manifest.lessons.find(l=>l.id===id),timing=data.report.lessons.find(l=>l.id===id);
    $('lessonTitle').textContent=l.title;$('summary').textContent=`${id} · ${timing.questionCount} câu hỏi · ước tính ${(timing.estimatedSecondsAt110Wpm/60).toFixed(1)} phút ở 110 từ/phút; chưa đo audio`;
    $('sections').replaceChildren(...list.map(s=>{const o=node('option',titles[s.component]);o.value=s.id;return o;}));
    renderFamily();renderSection();
  }
  const lessonButtons=data.report.lessons.map(l=>{const b=node('button',l.id);b.dataset.lesson=l.id;b.title=data.manifest.lessons.find(m=>m.id===l.id).title;b.onclick=()=>selectLesson(l.id);return b;});
  $('lessonNav').replaceChildren(...lessonButtons);
  $('sections').onchange=()=>{script=byId.get($('sections').value);renderSection();};
  $('bilingual').onchange=syncTranslations;
  $('wait').onclick=wait;$('reveal').onclick=reveal;
  $('previous').onclick=()=>{if(qi>0){qi--;renderQuestion();}};
  $('next').onclick=()=>{if(qi<questions().length-1){qi++;renderQuestion();}};
  $('sourceButton').onclick=()=>{sourceOpen=!sourceOpen;renderSource();};
  document.addEventListener('visibilitychange',()=>{if(document.hidden)pause();});
  window.addEventListener('pagehide',()=>{pause();stop();});
  $('vocabulary').replaceChildren(...data.inventory.words.map(w=>{
    const box=node('article',undefined,'word');box.append(node('strong',w.lemma),node('span',w.ipa,'ipa'),node('p',w.glossVi));return box;
  }));
  selectLesson(data.report.lessons[0].id);
})();
