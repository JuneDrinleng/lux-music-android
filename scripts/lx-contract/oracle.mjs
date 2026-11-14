// Test-only oracle. Loads pinned upstream code unchanged; no server or npm install.
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { stripTypeScriptTypes, createRequire } from 'node:module';
import { createHash, generateKeyPairSync, createPrivateKey } from 'node:crypto';
import { execFileSync } from 'node:child_process';

export const LX_SHA = 'd47aca4284a7c4d9ef755df1f44fb0b0a5b2af36';
export const M2C_INTEGRITY = 'sha512-jOOPKWTE5LMfuxz8D1o186ex+kJl7FgRz033Z371kvyfZDyCn+qhtrsIvgf4kPUf4zYAWM2SU4GFWbA7F2U+Ag==';
export const M2C_DIST_SHA = 'e60f226619e81a0c700aeb72ede193cd14ca5e0d119c7830dd97fb89381b623e';
const lx = process.env.LUX_LX_SOURCE;
if (!lx) throw Error('Set LUX_LX_SOURCE to the pinned checkout');
if (execFileSync('git',['-C',lx,'rev-parse','HEAD'],{encoding:'utf8'}).trim() !== LX_SHA) throw Error('Unexpected upstream revision');
execFileSync('git',['-C',lx,'diff','--quiet','HEAD','--','src','package-lock.json']);
const locked = JSON.parse(fs.readFileSync(path.join(lx,'package-lock.json'))).packages['node_modules/message2call'];
if (locked.version !== '0.1.3' || locked.integrity !== M2C_INTEGRITY) throw Error('Unexpected message2call lock');
const releaseDist = path.resolve('.contract-upstream/message2call.cjs');
if(createHash('sha256').update(fs.readFileSync(releaseDist)).digest('hex') !== M2C_DIST_SHA) throw Error('Run prepare.py to verify the locked release artifact');
const require = createRequire(import.meta.url);
const { createMsg2call } = require(releaseDist);
const clone = value => structuredClone(value);
const md5 = value => createHash('md5').update(JSON.stringify(value)).digest('hex');
const empty = () => ({defaultList:[],loveList:[],userList:[]});
const fixturePath = path.resolve('data/src/test/resources/lx/vectors.json');

export function keys() {
  if (fs.existsSync(fixturePath)) { const {purpose,publicDer,privateDer}=JSON.parse(fs.readFileSync(fixturePath)).keys;return {purpose,publicDer,privateDer}; }
  const pair = generateKeyPairSync('rsa',{modulusLength:2048});
  return {
    purpose:'PUBLIC TEST MATERIAL ONLY; never use as a real account or key',
    publicDer:pair.publicKey.export({type:'spki',format:'der'}).toString('base64'),
    privateDer:pair.privateKey.export({type:'pkcs8',format:'der'}).toString('base64'),
  };
}

export function privateKey(material) { return createPrivateKey({key:Buffer.from(material.privateDer,'base64'),format:'der',type:'pkcs8'}); }

export async function harness() {
  const state = {
    device:{clientId:'fixture-client',key:Buffer.from('0123456789abcdef').toString('base64'),deviceName:'fixture',isMobile:true},
    server:empty(),client:empty(),snapshot:null,deviceSnapshot:null,
    calls:[],moduleError:null,failSet:false,mode:'cancel',location:'bottom',closeCallbacks:[],
  };
  const listManage = {
    getListData:async()=>clone(state.server),
    getCurrentListInfoKey:async()=>md5(state.server),
    getDeviceCurrentSnapshotKey:async()=>state.deviceSnapshot,
    updateDeviceSnapshotKey:async(_id,key)=>{state.deviceSnapshot=key;},
    createSnapshot:async()=>md5(state.server),
    snapshotDataManage:{getSnapshot:async()=>clone(state.snapshot)},
  };
  const dataManage = {getClientKeyInfo:id=>id===state.device.clientId?state.device:null,saveClientKeyInfo:key=>{state.device=key;}};
  const users = {
    getUserSpace:()=>({listManage,dataManage}),
    getUserConfig:()=>({'list.addMusicLocationType':state.location}),
    getUserName:id=>id===state.device.clientId?'fixture-user':null,
    setUserName:()=>{},
    createClientKeyInfo:(deviceName,isMobile)=>({...state.device,deviceName,isMobile}),
  };
  const cache = new Map();
  const sandbox = {Buffer,setTimeout,clearTimeout,setImmediate,queueMicrotask,console:{log(){},error(){}}};
  sandbox.global=sandbox;
  sandbox.lx={config:{'proxy.enabled':false,serverName:'测试 LX'}};
  sandbox.event_list={list_data_overwrite:async(_user,data)=>{state.server=clone(data);}};
  const context=vm.createContext(sandbox);
  const modules = new Map();
  async function synthetic(id, values) {
    if (modules.has(id)) return modules.get(id);
    const mod = new vm.SyntheticModule(Object.keys(values),function(){for(const [key,value] of Object.entries(values))this.setExport(key,value);},{context,identifier:id});
    modules.set(id,mod);return mod;
  }
  async function load(relative) {
    const id=path.join(lx,relative);
    if(modules.has(id))return modules.get(id);
    const source=stripTypeScriptTypes(fs.readFileSync(id,'utf8'),{mode:'transform',sourceUrl:id});
    const mod=new vm.SourceTextModule(source,{context,identifier:id});modules.set(id,mod);
    await mod.link(async(specifier,ref)=>{
      if(specifier.startsWith('node:'))return synthetic(specifier,await import(specifier));
      if(specifier==='@/constants')return load('src/constants.ts');
      if(specifier==='@/utils/tools')return load('src/utils/tools.ts');
      if(specifier==='@/utils')return load('src/utils/index.ts');
      if(specifier==='@/utils/cache')return synthetic('cache',{default:cache});
      if(specifier==='@/user'||specifier==='../user/data')return synthetic('users',users);
      if(specifier==='./log4js')return synthetic('logger',{syncLog:{info(){}}});
      if(specifier==='@/modules') {
        const list=await load('src/modules/list/sync/sync.ts');
        if(list.status!=='evaluated')await list.evaluate();
        return synthetic('sync-modules',{
          featureVersion:{list:1,dislike:1},
          modules:{list:{sync:async socket=>{try{await list.namespace.sync(socket);}catch(e){state.moduleError=e.message;throw e;}}}},
        });
      }
      if(specifier.startsWith('.')) {
        let file=path.resolve(path.dirname(ref.identifier),specifier);
        if(!file.endsWith('.ts'))file+='.ts';
        return load(path.relative(lx,file));
      }
      throw Error('Unsupported fixture dependency: '+specifier);
    });
    return mod;
  }
  async function evaluated(file) {const mod=await load(file);if(mod.status!=='evaluated')await mod.evaluate();return mod.namespace;}
  const tools=await evaluated('src/utils/tools.ts');
  const constants=await evaluated('src/constants.ts');
  const auth=await evaluated('src/server/auth.ts');
  const sync=await evaluated('src/server/sync/sync.ts');
  return {state,tools,constants,auth,sync};
}

export async function authenticate(h, headers) {
  let status;let body;
  await h.auth.authCode({headers,socket:{remoteAddress:'127.0.0.1'}},{writeHead(value){status=value;},end(value){body=value;}},[{name:'fixture-user',password:'fixture-连接码-123'}]);
  return {status,body,mobile:h.state.device.isMobile,deviceName:h.state.device.deviceName};
}

export async function synchronize(h, input) {
  Object.assign(h.state,{server:clone(input.server),client:clone(input.client),snapshot:input.snapshot?clone(input.snapshot):null,mode:input.mode,failSet:!!input.failSet});
  h.state.deviceSnapshot=input.snapshot?'fixture-snapshot':null;
  const socket={
    userInfo:{name:'fixture-user'},keyInfo:{clientId:'fixture-client'},feature:{},moduleReadys:{list:false},
    onClose:fn=>h.state.closeCallbacks.push(fn),broadcast(){},
    remote:{getEnabledFeatures:async(type,versions)=>{h.state.calls.push('getEnabledFeatures');if(type!=='server'||versions.list!==1)throw Error('bad feature probe');return {list:{skipSnapshot:false}};},finished:async()=>{h.state.calls.push('finished');}},
    remoteQueueList:{
      list_sync_get_list_data:async()=>{h.state.calls.push('get_list');return clone(h.state.client);},
      list_sync_get_md5:async()=>md5(h.state.client),
      list_sync_get_sync_mode:async()=>{h.state.calls.push('choose_mode');return h.state.mode;},
      list_sync_set_list_data:async data=>{h.state.calls.push('set_list');if(h.state.failSet)throw Error('fixture disconnected');h.state.client=clone(data);},
      list_sync_finished:async()=>{h.state.calls.push('list_finished');},
    },
  };
  await h.sync.sync(socket);
  return {server:h.state.server,client:h.state.client,ready:socket.moduleReadys.list,error:h.state.moduleError,calls:h.state.calls};
}

export async function query(input) {
  const h=await harness();
  switch(input.operation) {
    case 'auth':return authenticate(h,input.headers);
    case 'socket':try{await h.auth.authConnect({url:input.url,headers:{},socket:{remoteAddress:'127.0.0.1'}});return {accepted:true};}catch{return {accepted:false};}
    case 'crypto':return {aes:h.tools.aesDecrypt(input.aes,h.state.device.key),rsa:h.tools.rsaDecrypt(Buffer.from(input.rsa,'base64'),privateKey(keys())).toString()};
    case 'frame':return {text:await h.tools.decryptMsg(null,input.frame)};
    case 'sync':return synchronize(h,input);
    case 'rpc':{
      const request=JSON.parse(await h.tools.decryptMsg(null,input.frame));
      let resolve;const response=new Promise(r=>{resolve=r;});
      const peer=createMsg2call({funcsObj:{echo:(...args)=>args,'literal:name':value=>value,empty(){},fail(){throw Error('fixture rejection');}},sendMessage:message=>resolve(message),timeout:1000});
      peer.message(request);const reply=await response;peer.destroy();return {frame:await h.tools.encryptMsg(null,JSON.stringify(reply))};
    }
    default:throw Error('Unknown test operation');
  }
}

if(process.argv[2]==='query') {
  const input=JSON.parse(fs.readFileSync(0,'utf8'));
  process.stdout.write(JSON.stringify(await query(input)));
}
