const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync(require.resolve('../www/index.html'),'utf8');
test('listening starts learning without completing a lesson or triggering recall',()=>{
 const c=vm.createContext({S:{},goalOf:()=>({days:1}),goalCalc:()=>({done:1})});
 vm.runInContext(html.slice(html.indexOf('function albumInfo('),html.indexOf('function progCount(')),c);
 const t={st:{sec:1}};
 assert.equal(c.albumInfo('test',[t]).status,'learn');assert.equal(c.trkSt(t),'learn');
 assert.equal(c.albumInfo('test',[{st:{}}]).status,'new');
 t.st.done=true;assert.equal(c.albumInfo('test',[t]).status,'done');assert.equal(c.trkSt(t),'done');
 const body=html.slice(html.indexOf('function renderLib(){'),html.indexOf("$('#libQ').addEventListener"));assert.ok(!body.includes('memAsk('));
});
test('review keeps explicit completion scopes and legacy individual lessons',()=>{
 const rows=[{t:{id:'a',name:'a',st:{reviewScope62:{key:'folder:p',name:'p'}}},due:30,completedAt:1,memory:{id:'a'}},{t:{id:'b',name:'b',st:{reviewScope62:{key:'folder:p',name:'p'}}},due:10,completedAt:2,memory:{id:'b'}},{t:{id:'c',name:'c',st:{}},due:20,completedAt:3,memory:{}}];
 const c=vm.createContext({tracks:[],lessonReviewItems:()=>rows});vm.runInContext(html.slice(html.indexOf('function reviewGroupKey53('),html.indexOf('function scheduledReview53(')),c);
 const groups=c.reviewGroups53();assert.equal(groups.length,2);assert.equal(groups[0].key,'folder:p');assert.equal(groups[0].items.length,2);assert.equal(groups[0].due,10);assert.equal(groups[1].key,'#c');
});
