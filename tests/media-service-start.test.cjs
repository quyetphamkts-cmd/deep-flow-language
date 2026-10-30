const {test}=require('node:test');
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),os=require('node:os'),vm=require('node:vm');
const patch=fs.readFileSync(path.join(__dirname,'../scripts/patch-media-session.js'),'utf8');
const dir=path.join(__dirname,'../node_modules/@jofr/capacitor-media-session/android/src/main/java/io/github/jofr/capacitor/mediasessionplugin');
function source(){return fs.readFileSync(path.join(dir,'MediaSessionService.java'),'utf8');}
test('foreground promotion happens before custom-action early return and media dispatch',()=>{
 const s=source(),start=s.slice(s.indexOf('public int onStartCommand'),s.indexOf('public void setPlaybackState'));
 assert.ok(start.indexOf('dfEnsureForeground()')<start.indexOf('plugin.actionCallback'));
 assert.ok(start.indexOf('dfEnsureForeground()')<start.indexOf('MediaButtonReceiver.handleIntent'));
 assert.match(start,/if \(mediaSession != null\) MediaButtonReceiver/);
 assert.doesNotMatch(start,/super.onStartCommand/);
 const bootstrap=s.slice(s.indexOf('private boolean dfEnsureForeground'),s.indexOf('public int onStartCommand'));
 assert.match(bootstrap,/createNotificationChannel/);assert.match(bootstrap,/setSmallIcon/);assert.match(bootstrap,/FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK/);
 assert.match(bootstrap,/catch \(Exception error\)[\s\S]*stopSelf\(\);[\s\S]*return false/);
});
test('orphan start is bounded and successful connection cancels its timeout',()=>{
 const s=source();assert.match(s,/dfOrphanStop = \(\) -> \{ if \(plugin == null\) destroy\(\); \}/);
 assert.match(s,/if \(plugin == null\) dfStartHandler.postDelayed\(dfOrphanStop, 15000\)/);
 assert.match(s,/connectAndInitialize[\s\S]*?removeCallbacks\(dfOrphanStop\);\n        this.plugin = plugin/);
 assert.match(s,/onDestroy\(\) \{ dfStartHandler.removeCallbacks\(dfOrphanStop\)/);
});
test('postinstall media patch remains idempotent',()=>{
 const files=new Map(fs.readdirSync(dir).filter(n=>n.endsWith('.java')).map(n=>[path.join(dir,n),fs.readFileSync(path.join(dir,n),'utf8')]));
 const before=new Map(files),fakeFs={existsSync:p=>files.has(p),readFileSync:p=>{assert.ok(files.has(p));return files.get(p)},writeFileSync:(p,s)=>files.set(p,s)};
 const run=()=>vm.runInNewContext(patch,{require:n=>n==='fs'?fakeFs:require(n),__dirname:path.join(__dirname,'../scripts'),console:{log:()=>{}},process:{exit:()=>assert.fail('unexpected skip')}});
 run();run();assert.deepEqual(files,before);
});
