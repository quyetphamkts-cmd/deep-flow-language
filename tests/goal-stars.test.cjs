'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const html=fs.readFileSync(path.join(__dirname,'../www/index.html'),'utf8');
const helper=html.slice(html.indexOf('function goalStarCounts55'),html.indexOf('function goalLine(a)'));
const calc=html.slice(html.indexOf('function albDaySec(a)'),html.indexOf('const hm=s=>'));
function context(){const c=vm.createContext({S:{albGoal:{}},tracks:[],unitKey:t=>t.album||'#'+t.id,GOAL_MIN:1800,GOAL_BEST:7200,gdk:d=>d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0')});vm.runInContext(calc+helper,c);return c;}
test('seven completed days exchange exactly for one gold star with remainder in blue',()=>{
 const c=context();for(const [days,gold,blue] of [[0,0,0],[1,0,1],[2,0,2],[6,0,6],[7,1,0],[8,1,1],[14,2,0],[18,2,4],[1000,142,6]])assert.deepEqual(JSON.parse(JSON.stringify(c.goalStarCounts55(days))),{gold,blue});
 for(const n of [-1,NaN,Infinity])assert.equal(c.goalStarCounts55(n).gold,0);
});
test('goal awards one star per completed date from existing listening data, not per track or two-hour session',()=>{
 const c=context();c.S.albGoal.A={days:7,start:'2026-01-02'};c.tracks=[{id:'a',album:'A',st:{ds:{'2026-01-01':7200,'2026-01-02':900,'2026-01-03':1799,'2026-01-04':7200}}},{id:'b',album:'A',st:{ds:{'2026-01-02':900}}},{id:'other',album:'B',st:{ds:{'2026-01-05':7200}}}];
 assert.equal(c.goalCalc('A',c.goalOf('A')).done,2);assert.equal(c.goalCalc('A',c.goalOf('A')).best,1);
 const before=JSON.stringify({s:c.S,t:c.tracks});const markup=c.goalStars55('A');assert.match(markup,/class="goalStar55 blue" data-count="2"/);assert.doesNotMatch(markup,/goalStar55 gold/);assert.equal(c.goalStars55('absent'),'');c.goalStars55('A');assert.equal(JSON.stringify({s:c.S,t:c.tracks}),before);
});
test('gold renders first, numbers are inside SVG, and zero remainder has no empty blue badge',()=>{
 const c=context();c.goalOf=()=>({});c.goalCalc=()=>({done:18});let h=c.goalStars55('A');assert.ok(h.indexOf('goalStar55 gold')<h.indexOf('goalStar55 blue'));assert.match(h,/<text[^>]*>2<\/text>/);assert.match(h,/<text[^>]*>4<\/text>/);c.goalCalc=()=>({done:14});h=c.goalStars55('A');assert.match(h,/data-count="2"/);assert.doesNotMatch(h,/goalStar55 blue/);
});
