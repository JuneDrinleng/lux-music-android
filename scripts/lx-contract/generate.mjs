import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {harness,keys,authenticate,synchronize,LX_SHA,M2C_INTEGRITY,M2C_DIST_SHA,query,privateKey} from './oracle.mjs';

const output='data/src/test/resources/lx/vectors.json';
const old=fs.existsSync(output)?JSON.parse(fs.readFileSync(output)):null;
const material=keys();
const h=await harness();
const code='fixture-连接码-123';
const derived=Buffer.from(createHash('md5').update(code).digest('hex').slice(0,16)).toString('base64');
const publicBody=material.publicDer;
const initialText=`${h.constants.SYNC_CODE.authMsg}\n${publicBody}\n测试 Android\nlx_music_mobile`;
const initial=await authenticate(h,{m:h.tools.aesEncrypt(initialText,derived)});
if(initial.status!==200||!initial.mobile)throw Error('Upstream initial auth failed');
const decoded=JSON.parse(h.tools.rsaDecrypt(Buffer.from(initial.body,'base64'),privateKey(material)));
const rsaCipher=old?.auth.rsaCiphertext||initial.body;
if(h.tools.rsaDecrypt(Buffer.from(rsaCipher,'base64'),privateKey(material)).toString()!==JSON.stringify(decoded))throw Error('Stored randomized RSA vector is invalid');
const aes=[];
for(const text of ['', '1234567890123456','中文与 emoji 🎵','lx-music connect'])aes.push({text,key:h.state.device.key,ciphertext:h.tools.aesEncrypt(text,h.state.device.key)});
const frames=[];
for(const text of ['{"name":"fixture","path":["echo"],"data":[]}', 'a'.repeat(1024),'a'.repeat(1025),'🎵'.repeat(512),'🎵'.repeat(513),'中文'.repeat(900)])frames.push({text,frame:await h.tools.encryptMsg(null,text),compressed:text.length>1024});
const jsonCases=[];
function jsonCase(name,input){const value=JSON.parse(input);const canonical=JSON.stringify(value);jsonCases.push({name,input,canonical,md5:createHash('md5').update(canonical).digest('hex')});}
jsonCase('indices-and-unicode','{"10":"ten","2":"two","01":"not-index","4294967295":"not-index","u":"\\ud800","text":"音乐🎵\\n\\u0000\\u2028\\u2029"}');
jsonCase('number-boundaries','[-0,1.0,1e-6,1e-7,1e20,1e21,1e23,1000000000000000128,9007199254740993,2.2250738585072014e-308,5e-324,1.7976931348623157e308,1e400]');
let seed=0x123456789abcdefn;const numberTokens=[];
for(let index=0;index<160;index++){
 seed=BigInt.asUintN(64,seed*6364136223846793005n+1442695040888963407n);
 const bytes=Buffer.alloc(8);bytes.writeBigUInt64BE(seed);const value=bytes.readDoubleBE();
 if(Number.isFinite(value))numberTokens.push(value.toPrecision(17));
}
jsonCase('deterministic-ieee754-corpus','['+numberTokens.join(',')+']');
const song=(id,name)=>({id,name,singer:'歌手',source:'kw',interval:null,meta:{songId:id,albumName:'专辑',qualitys:[],_qualitys:{},unknown:{'10':'x','2':'y'}}});
const list=(id,name,music,time=null)=>({id,name,locationUpdateTime:time,list:music});
const server={defaultList:[song('1','server-one'),song('2','server-two')],loveList:[],userList:[list('same','server',[song('1','server-one')],10),list('server-only','server-only',[])]};
const client={defaultList:[song('1','client-one'),song('3','client-three')],loveList:[],userList:[list('same','client',[song('3','client-three')],20),list('client-only','client-only',[])]};
jsonCase('snapshot',JSON.stringify({...client,extraField:{keep:true}}));
const empty=()=>({defaultList:[],loveList:[],userList:[]});
const syncCases=[];
async function addSync(name,input){const instance=await harness();syncCases.push({name,input,result:await synchronize(instance,input)});}
for(const mode of Object.keys(h.constants.TRANS_MODE))await addSync(mode,{server,client,mode});
await addSync('empty-client',{server,client:empty(),mode:'cancel'});
await addSync('empty-server',{server:empty(),client,mode:'cancel'});
const snapshot={defaultList:[song('1','one'),song('2','two')],loveList:[],userList:[]};
await addSync('three-way-deletions',{server:{...empty(),defaultList:[song('1','one'),song('4','server-new')]},client:{...empty(),defaultList:[song('2','two'),song('5','client-new')]},snapshot,mode:'cancel'});
await addSync('already-current',{server,client:server,snapshot:server,mode:'cancel'});
await addSync('response-failure',{server,client,mode:'merge_local_remote',failSet:true});
const failed=syncCases.at(-1).result;
await addSync('reconnect-reconcile',{server:failed.server,client:failed.client,mode:'merge_local_remote'});
const rpc=[];
for(const request of [
 {name:'fixture-echo',path:['echo'],data:[null,{unknown:'🎵'}]},
 {name:'fixture-empty',path:['empty'],data:[]},
 {name:'fixture-fail',path:['fail'],data:[]},
 {name:'fixture-literal',path:['literal:name'],data:[7]},
]){
 const result=await query({operation:'rpc',frame:JSON.stringify(request)});rpc.push({request,response:JSON.parse(await h.tools.decryptMsg(null,result.frame))});
}
const sourceHashes={};
for(const file of ['src/constants.ts','src/utils/tools.ts','src/utils/index.ts','src/server/auth.ts','src/server/sync/sync.ts','src/modules/list/utils.ts','src/modules/list/sync/sync.ts'])sourceHashes[file]=createHash('sha256').update(fs.readFileSync(path.join(process.env.LUX_LX_SOURCE,file))).digest('hex');
sourceHashes['package-lock.json']=createHash('sha256').update(fs.readFileSync(path.join(process.env.LUX_LX_SOURCE,'package-lock.json'))).digest('hex');
const constants={hello:h.constants.SYNC_CODE.helloMsg,idPrefix:h.constants.SYNC_CODE.idPrefix,connect:h.constants.SYNC_CODE.msgConnect};
const vectors={constants,provenance:{lx:LX_SHA,message2call:{version:"0.1.3",integrity:M2C_INTEGRITY,distSha256:M2C_DIST_SHA},node:process.version,sourceHashes},keys:material,auth:{code,derived,initialText,initialHeader:h.tools.aesEncrypt(initialText,derived),rsaCiphertext:rsaCipher,decoded},aes,frames,jsonCases,rpc,syncCases,modes:h.constants.TRANS_MODE};
const rendered=JSON.stringify(vectors,null,2)+'\n';
if(process.argv.includes('--check')){
 if(!old||fs.readFileSync(output,'utf8')!==rendered)throw Error('Fixture drift: regenerate and review upstream-derived changes');
 console.log('Upstream fixtures unchanged; randomized RSA ciphertext separately validated');
}else{fs.writeFileSync(output,rendered);console.log('Generated upstream fixtures: '+syncCases.length+' sync cases, '+jsonCases.length+' JSON cases');}
