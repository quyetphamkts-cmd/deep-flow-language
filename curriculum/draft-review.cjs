'use strict';
const {normalizeManifest} = require('./manifest.cjs');
const {validateScript, createVocabularyIndex, auditCurriculum, scanText, planAudio} = require('./engine.cjs');
const words = text => (text.match(/[A-Za-z]+(?:'[A-Za-z]+)*/g) || []).length;
const spoken = script => script.segments.filter(s=>s.type!=='pause').map(s=>s.text).join(' ');

// Kiểm tra cấu trúc; ý nghĩa, chất lượng sư phạm và phê duyệt cần người rà soát.
function reviewDraft(manifest, bundle, inventory) {
  const m = normalizeManifest(manifest), errors = [], scripts = bundle?.scripts;
  if (bundle?.schemaVersion!==1 || bundle.status!=='authored_draft_pending_user_review' || !Array.isArray(scripts)) throw new Error('Gói bản nháp không hợp lệ');
  const authoredIds=new Set(scripts.map(s=>s?.lessonId));
  const authoredLessons=m.lessons.filter(l=>authoredIds.has(l.id));
  const expected=authoredLessons.flatMap(l=>l.components.map(c=>c.id));
  if (!expected.length || scripts.length!==expected.length || new Set(scripts.map(s=>s?.id)).size!==expected.length || scripts.some(s=>!expected.includes(s?.id))) throw new Error('Mỗi bài đã biên soạn phải có đủ các phần hợp lệ, không trùng ID');
  const byId = new Map(scripts.map(s=>[s.id,s]));
  for (const s of scripts) {
    const l=m.lessons.find(l=>l.id===s.lessonId), c=l?.components.find(c=>c.id===s.id);
    if (!c) {errors.push(`${s.id}: không thuộc bài khai báo`);continue;}
    errors.push(...validateScript(s,l,c));
    if (s.status!=='draft' || !s.segments?.length || s.provenance?.editorialApproval!=='pending') errors.push(`${s.id}: phải là nội dung nháp chờ duyệt`);
  }
  if (errors.length) throw new Error(errors.join('\n'));
  for (const s of scripts) {
    for (let i=0;i<s.segments.length;i++) {
      const q=s.segments[i];
      if (q.type!=='question') continue;
      const answer=s.segments[i+1];
      if (answer?.id!==q.answerId || answer?.type!=='answer') errors.push(`${s.id}/${q.id}: đáp án phải nằm ngay sau câu hỏi`);
      if (q.answerPolicy!=='model_not_exact_match') errors.push(`${s.id}/${q.id}: không dùng đáp án mẫu để chấm khớp nguyên văn`);
      if (!Array.isArray(q.sourceRefs) || !q.sourceRefs.length) {errors.push(`${s.id}/${q.id}: thiếu liên kết truyện`);continue;}
      for (const ref of q.sourceRefs) {
        const source=byId.get(ref?.scriptId), segment=source?.segments.find(p=>p.id===ref.segmentId);
        if (!source || source.lessonId!==s.lessonId || !['main_story','daily_adventure'].includes(source.component) || segment?.type!=='narration') errors.push(`${s.id}/${q.id}: liên kết truyện sai hoặc bắt buộc bài khác`);
      }
    }
  }
  const index=createVocabularyIndex(inventory);
  const selections={...bundle.wordDiscoveryByLesson};
  if(bundle.wordDiscoverySelection)selections.C01=bundle.wordDiscoverySelection;
  for(const id of Object.keys(selections))if(!authoredLessons.some(l=>l.id===id && l.kind==='core'))errors.push(`Word Discovery: bài ${id} chưa biên soạn hoặc không phải bài chính`);
  const selectionCounts={};
  for(const l of authoredLessons.filter(l=>l.kind==='core')){
    const items=selections[l.id],selection=new Set(),discovery=byId.get(`${l.id}-word_discovery`);
    if(!Array.isArray(items)||!items.length){errors.push(`${l.id}: thiếu lý do chọn Word Discovery`);continue;}
    const discoveryWords=new Set(scanText(spoken(discovery),index).filter(h=>h.id).map(h=>h.id));
    for (const w of items) {
      if (!w || !index.words.has(w.wordId) || !discoveryWords.has(w.wordId) || selection.has(w.wordId) || typeof w.reason!=='string' || !w.reason.trim()) errors.push(`${l.id} Word Discovery: thiếu từ/trùng/thiếu lý do cho ${w?.wordId}`);
      if(w)selection.add(w.wordId);
    }
    selectionCounts[l.id]=selection.size;
  }
  if (errors.length) throw new Error(errors.join('\n'));
  const audit=auditCurriculum(manifest,inventory,scripts);
  const sections=expected.map(id=>{
    const s=byId.get(id), text=spoken(s), count=words(text), plan=planAudio(s,{provider:'mock',model:'planning-only',voice:'unselected',language:'en',speed:1});
    return {id,spokenWords:count,questionCount:s.segments.filter(p=>p.type==='question').length,pauseSeconds:plan.pauseSeconds,
      estimatedSecondsAt110Wpm:Math.round(count/110*60+plan.pauseSeconds),
      estimatedRangeSeconds:[Math.round(count/120*60+plan.pauseSeconds),Math.round(count/100*60+plan.pauseSeconds)],
      measuredAudioSeconds:null,cacheKey:plan.cacheKey};
  });
  const lessons=authoredLessons.map(({id})=>{
    const ss=sections.filter(s=>s.id.startsWith(id+'-'));
    return {id,questionCount:ss.reduce((n,s)=>n+s.questionCount,0),spokenWords:ss.reduce((n,s)=>n+s.spokenWords,0),pauseSeconds:ss.reduce((n,s)=>n+s.pauseSeconds,0),
      estimatedSecondsAt110Wpm:ss.reduce((n,s)=>n+s.estimatedSecondsAt110Wpm,0),estimatedRangeSeconds:[0,1].map(i=>ss.reduce((n,s)=>n+s.estimatedRangeSeconds[i],0)),measuredAudioSeconds:null};
  });
  return {schemaVersion:1,contentVersion:bundle.contentVersion,publicationReady:false,canonicalVocabularyReady:false,structuralQaPassed:true,semanticQa:'pending_user_review',
    timingMethod:'spoken words / assumed WPM + explicit pauses; 100–120 WPM are planning assumptions, not measurements',sections,lessons,
    proposedVocabularyCount:index.words.size,wordDiscoveryCount:Object.values(selectionCounts).reduce((n,v)=>n+v,0),wordDiscoveryByLesson:selectionCounts,proposedInventoryAudit:audit,
    constraints:['Chưa duyệt nội dung, nguồn danh sách 3000 từ, IPA hoặc giọng đọc.','Liên kết câu hỏi chứng minh nguồn tồn tại, không tự chứng minh đáp án đúng về nghĩa.','Không tạo audio thật, chấm người học hoặc thay dữ liệu ứng dụng.']};
}

function renderDraft(manifest,bundle,inventory,options={}) {
  const report=reviewDraft(manifest,bundle,inventory), byId=new Map(bundle.scripts.map(s=>[s.id,s]));
  const selected=options.lessonIds||report.lessons.map(l=>l.id);
  if(!selected.length || new Set(selected).size!==selected.length || selected.some(id=>!report.lessons.some(l=>l.id===id)))throw new Error('Bài để xuất bản đọc chưa có nội dung hoặc bị trùng');
  const lessons=report.lessons.filter(l=>selected.includes(l.id));
  const usedWords=new Set(report.proposedInventoryAudit.lessons.filter(l=>selected.includes(l.id)).flatMap(l=>Object.keys(l.counts)));
  const titles={main_story:'Main Story',word_discovery:'Word Discovery',quick_response:'Quick Response',time_shift:'Time Shift',daily_adventure:'Daily Adventure'};
  const out=[`# ${lessons.map(l=>l.id).join(' · ')} — bản nháp nội dung ${bundle.contentVersion}`,'','**Chờ người dùng duyệt; chưa phát hành, chưa có giọng đọc thật.**','','Bản đọc này sinh từ `curriculum/drafts/v1/scripts.json`. Câu trả lời là mẫu, không yêu cầu người học nói đúng nguyên văn.','','Ước tính dùng giả định 100–120 từ/phút cộng khoảng chờ; chưa đo thời lượng audio.',''];
  for(const l of lessons){
    const title=manifest.lessons.find(x=>x.id===l.id).title;
    out.push(`## ${l.id} — ${title}`,'',`${l.questionCount} câu hỏi · ước tính ${(l.estimatedSecondsAt110Wpm/60).toFixed(1)} phút ở 110 từ/phút, khoảng ${(l.estimatedRangeSeconds[0]/60).toFixed(1)}–${(l.estimatedRangeSeconds[1]/60).toFixed(1)} phút theo giả định tốc độ.`, '');
    for(const meta of report.sections.filter(s=>s.id.startsWith(l.id+'-'))){
      const s=byId.get(meta.id);out.push(`### ${titles[s.component]}`,'');
      for(const p of s.segments){
        if(p.type==='question'){out.push(`**${p.id}. ${p.text}**`,``,`*Chờ ${p.answerPauseMs/1000} giây. Nguồn: ${p.sourceRefs.map(r=>r.scriptId+'/'+r.segmentId).join(', ')}.*`,'');}
        else if(p.type==='answer')out.push(`Đáp án mẫu: ${p.text}`,'');
        else if(p.type==='pause')out.push(`*[Nghỉ ${p.seconds} giây]*`,'');
        else out.push(`**[${p.id}]** ${p.text}`,'');
        if(p.translationVi)out.push(`*Tiếng Việt:* ${p.translationVi}`,'');
      }
    }
  }
  out.push('## Từ vựng đề xuất — chưa phải danh sách chuẩn 3.000 từ','','| Từ/cụm | IPA dự kiến (Mỹ) | Nghĩa trong bài |','|---|---|---|');
  for(const w of inventory.words.filter(w=>usedWords.has(w.id)))out.push(`| ${w.lemma} | ${w.ipa} | ${w.glossVi} |`);
  out.push('','## Vì sao chọn Word Discovery?','');
  for(const l of lessons){
    const items=l.id==='C01'?bundle.wordDiscoverySelection:bundle.wordDiscoveryByLesson?.[l.id];
    if(!items?.length)continue;
    out.push(`### ${l.id}`,'');
    for(const w of items)out.push(`- **${inventory.words.find(i=>i.id===w.wordId).lemma}:** ${w.reason}`);
    out.push('');
  }
  out.push('','Các lý do trên là giả thuyết biên soạn; chưa có dữ liệu cho thấy người học thực tế hay quên các từ này.','');
  return out.join('\n');
}
module.exports={reviewDraft,renderDraft};
