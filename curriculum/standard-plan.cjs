'use strict';
const {createVocabularyIndex,scanText}=require('./engine.cjs');
const CORE=Array.from({length:12},(_,i)=>'C'+String(i+1).padStart(2,'0'));
function coverage(catalog,bundle){
 const index=createVocabularyIndex(catalog),byWord=new Map(catalog.words.map(w=>[w.id,new Map()]));
 for(const s of bundle.scripts.filter(s=>CORE.includes(s.lessonId)))for(const seg of s.segments){
  if(seg.type==='pause')continue;
  for(const hit of scanText(seg.text,index))if(hit.id){const byLesson=byWord.get(hit.id);if(!byLesson.has(s.lessonId))byLesson.set(s.lessonId,[]);const ev=byLesson.get(s.lessonId);if(!ev.some(e=>e.scriptId===s.id&&e.segmentId===seg.id))ev.push({scriptId:s.id,segmentId:seg.id,form:hit.form});}
 }
 return byWord;
}
function validateCatalog(c){
 if(c?.schemaVersion!==1||c.status!=='approved_for_beta_baseline'||c.license!=='CC-BY-SA-4.0'||!Array.isArray(c.words)||c.words.length!==3000)throw Error('Danh sách chuẩn beta phải có đúng 3000 đầu mục và nguồn/giấy phép');
 createVocabularyIndex(c);
 if(new Set(c.words.map(w=>w.lemma)).size!==3000||c.words.some(w=>!/^[a-z]+$/.test(w.lemma)||!c.sources?.some(s=>s.id===w.sourceId)))throw Error('Trùng đầu mục, cụm hoặc thiếu nguồn');
 for(const [id,n] of [['ngsl-1.01',2801],['editorial-199',199]])if(c.words.filter(w=>w.sourceId===id).length!==n||!c.sources.find(s=>s.id===id)?.sha256)throw Error('Sai số lượng/thiếu mã băm nguồn');
}
function makePlan(catalog,bundle){
 validateCatalog(catalog);const hits=coverage(catalog,bundle),assigned=new Map(),slots=new Map(CORE.map(id=>[id,[]]));
 // Place observed lemmas first. Specific words get priority; broad/common words can move to another observed lesson.
 function place(id,visited){for(const lesson of hits.get(id).keys()){
  if(visited.has(lesson))continue;visited.add(lesson);const slot=slots.get(lesson);
  if(slot.length<250){slot.push(id);assigned.set(id,lesson);return true;}
  for(let i=slot.length-1;i>=0;i--){const displaced=slot[i];if(place(displaced,visited)){slot[i]=id;assigned.set(id,lesson);return true;}}
 }return false;}
 for(const w of [...catalog.words].sort((a,b)=>hits.get(a.id).size-hits.get(b.id).size||catalog.words.indexOf(a)-catalog.words.indexOf(b)))if(hits.get(w.id).size)place(w.id,new Set());
 for(const w of catalog.words)if(!assigned.has(w.id)){
  const themeLesson=w.theme==='family_home'?['C01','C02','C03','C10','C12']:w.theme==='food_health_nature'?['C04','C05','C06','C11']:w.theme==='work_travel_city'?['C07','C08','C09']:[];
  const lesson=[...themeLesson,...CORE].find(id=>slots.get(id).length<250);slots.get(lesson).push(w.id);assigned.set(w.id,lesson);
 }
 return {schemaVersion:1,version:1,status:'allocated_for_draft_revision',catalogVersion:catalog.version,policy:'One target lesson per headword, 250 per core. A teaching target can have incidental earlier exposure. Observed-word placement is a drafting suggestion, not semantic approval or mastery. Missing targets remain explicitly missing.',lessons:CORE.map(id=>({id,wordIds:slots.get(id)}))};
}
function auditStandard(catalog,plan,bundle){
 validateCatalog(catalog);if(plan?.schemaVersion!==1||plan.catalogVersion!==catalog.version||plan.status!=='allocated_for_draft_revision'||plan.lessons?.length!==12)throw Error('Bảng phân bổ không hợp lệ');
 const hits=coverage(catalog,bundle),used=new Set();
 const lessons=plan.lessons.map((l,i)=>{
  if(l.id!==CORE[i]||!Array.isArray(l.wordIds)||l.wordIds.length!==250)throw Error('Cần đúng thứ tự 12 bài, mỗi bài 250 mục');
  const words=l.wordIds.map(id=>{if(used.has(id)||!hits.has(id))throw Error('Từ bị phân bổ trùng hoặc không có trong chuẩn');used.add(id);const ev=hits.get(id);return {id,evidence:ev.get(l.id)||[],observedLessons:[...ev.keys()],firstObservedLesson:CORE.find(c=>ev.has(c))||null};});
  return {id:l.id,targetCount:words.length,observedTargetCount:words.filter(w=>w.evidence.length).length,missingTargetIds:words.filter(w=>!w.evidence.length).map(w=>w.id),incidentalEarlierCount:words.filter(w=>w.firstObservedLesson&&w.firstObservedLesson<l.id).length,words};
 });
 if(used.size!==3000)throw Error('Phân bổ không phủ đủ 3000 mục');
 const present=[...hits].filter(([,v])=>v.size).length,observedAllocated=lessons.reduce((n,l)=>n+l.observedTargetCount,0);
 return {schemaVersion:1,catalogReady:true,allocationReady:true,textCoverageReady:observedAllocated===3000,publicationReady:false,totalHeadwords:3000,totalAssigned:used.size,observedAnywhereInCore:present,absentFromCore:3000-present,observedInTargetLesson:observedAllocated,notYetInTargetLesson:3000-observedAllocated,coverageMethod:'Explicit forms, English spoken core segments; ambiguous forms excluded at import. Matches are lexical evidence, not verified senses or learning outcomes.',lessons};
}
function renderStandardReport(catalog,plan,bundle){const r=auditStandard(catalog,plan,bundle);return ['# Danh sách chuẩn beta và phân bổ 3.000 đầu mục','','Đã chốt nền biên soạn beta: NGSL **1.01** (2.801 đầu mục) + 199 mục bổ sung do Deep Flow chọn theo chủ đề. Không gọi đây là NGSL 1.2 hoặc 3.000 từ có tần suất cao nhất. Nguồn, mã băm, thay đổi và CC BY-SA 4.0 nằm trong `curriculum/sources/ngsl-1.01/NOTICE.md` và `vocabulary.standard.json`.','','Mỗi từ chỉ có một bài mục tiêu, mỗi bài 250 mục. Đây là phân bổ để hiệu chỉnh: lần đầu tập trung học có thể sau một lần gặp thoáng qua. Không khẳng định mỗi bài đã dạy đủ 250 từ mới hoặc đã đủ audio.', '',`Đã khớp ${r.observedAnywhereInCore}/3000 mục trong core; ${r.absentFromCore} mục chưa khớp ở bất kỳ bài chính nào. ${r.observedInTargetLesson}/3000 mục đã khớp trong chính bài được phân bổ. Chưa duyệt đúng nghĩa/loại từ từng chỗ.`, '', '| Bài | Đã phân bổ | Khớp trong bài mục tiêu | Cần bổ sung vào bài |','|---|---:|---:|---:|',...r.lessons.map(l=>`| ${l.id} | 250 | ${l.observedTargetCount} | ${l.missingTargetIds.length} |`),'','Từ/cụm mẫu 173 mục và 24 họ từ vẫn là công cụ giải thích/IPA đang rà soát; không dùng số đó thay cho bảng chuẩn này. Không tự gộp thêm họ từ cho 3.000 đầu mục nếu chưa có dữ liệu.','','Danh sách và phân bổ: đạt kiểm tra cấu trúc. Bao phủ văn bản và sẵn sàng xuất bản: **chưa đạt**. Bài mở rộng không được tính bù cho core. Các mục chưa có trong bài hiển thị rõ trong app để duyệt.',''].join('\n');}
module.exports={validateCatalog,makePlan,auditStandard,renderStandardReport};
