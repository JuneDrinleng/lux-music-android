// Test-only reverse proxy and a second protocol client. The deployed LX server is unchanged.
import http from 'node:http';
import https from 'node:https';
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { createHash, generateKeyPairSync, createCipheriv, createDecipheriv, privateDecrypt, constants } from 'node:crypto';
import { gzipSync, gunzipSync } from 'node:zlib';
import { execFileSync } from 'node:child_process';

const deployment = path.resolve('.contract-upstream/lx-deployment');
const require = createRequire(path.join(deployment,'package.json'));
const { WebSocket, WebSocketServer } = require('ws');
const { createMsg2call } = require('message2call');
const { ListDataManage } = require(path.join(deployment,'server/modules/list/listDataManage.js'));
const upstream = 'http://127.0.0.1:19527';
const code = 'public-fixture-a'; // Deliberately public test material, not a deployment credential.
const empty = () => ({defaultList:[],loveList:[],userList:[]});
const encode = value => {const text=JSON.stringify(value);return text.length>1024?'cg_'+gzipSync(text).toString('base64'):text};
const decode = text => JSON.parse(text.startsWith('cg_')?gunzipSync(Buffer.from(text.slice(3),'base64')).toString():text);
const encrypt = (text,key) => {const cipher=createCipheriv('aes-128-ecb',key,null);return Buffer.concat([cipher.update(text),cipher.final()]).toString('base64')};
let peer;
let dropNextReply=false;
const stats={clientActions:0,droppedReplies:0};
const proxyClients=new Set();
const proxy = http.createServer((request,response)=>{
  if(!request.url.startsWith('/proxy/')){response.writeHead(404).end();return}
  const forwarded=http.request(upstream+request.url.slice(6),{method:request.method,headers:request.headers},result=>{
    response.writeHead(result.statusCode,result.headers);result.pipe(response);
  });
  forwarded.on('error',()=>response.writeHead(502).end());request.pipe(forwarded);
});
const sockets=new WebSocketServer({noServer:true,maxPayload:8*1024*1024});
proxy.on('upgrade',(request,socket,head)=>{
  if(!request.url.startsWith('/proxy/socket?')){socket.destroy();return}
  sockets.handleUpgrade(request,socket,head,client=>{
    const remote=new WebSocket(upstream.replace('http:','ws:')+request.url.slice(6));
    let lostName;
    proxyClients.add(client);
    client.on('message',data=>{
      const text=data.toString();
      if(text!=='ping'){
        const message=decode(text);
        if(message.path?.[0]==='onListSyncAction'){
          stats.clientActions++;
          if(dropNextReply){dropNextReply=false;lostName=message.name}
        }
      }
      if(remote.readyState===WebSocket.OPEN)remote.send(text);else client.terminate();
    });
    remote.on('message',data=>{
      const text=data.toString();
      if(lostName&&text!=='ping'&&decode(text).name===lostName){
        stats.droppedReplies++;lostName=null;client.terminate();remote.terminate();return;
      }
      if(client.readyState===WebSocket.OPEN)client.send(text);
    });
    remote.on('ping',()=>{if(client.readyState===WebSocket.OPEN)client.ping()});
    client.on('close',()=>{proxyClients.delete(client);remote.terminate()});
    client.on('error',()=>remote.terminate());
    remote.on('close',()=>client.terminate());
    remote.on('error',()=>client.terminate());
  });
});

async function makePeer(){
  const manager=new ListDataManage({getSnapshotInfo:async()=>({latest:null})});
  await new Promise(setImmediate);
  const keys=generateKeyPairSync('rsa',{modulusLength:2048});
  const publicKey=keys.publicKey.export({type:'spki',format:'der'}).toString('base64');
  const initial=encrypt('lx-music auth::\n'+publicKey+'\nNode fixture\nlx_music_mobile',
    Buffer.from(createHash('md5').update(code).digest('hex').slice(0,16)));
  const auth=await fetch(upstream+'/ah',{headers:{m:initial}});
  if(!auth.ok)throw Error('Fixture peer authentication failed');
  const credentials=JSON.parse(privateDecrypt({key:keys.privateKey,padding:constants.RSA_PKCS1_OAEP_PADDING,oaepHash:'sha1'},
    Buffer.from(await auth.text(),'base64')).toString());
  const query=new URLSearchParams({i:credentials.clientId,t:encrypt('lx-music connect',Buffer.from(credentials.key,'base64'))});
  const socket=new WebSocket(upstream.replace('http:','ws:')+'/socket?'+query);
  let listReady=false,resolveReady,rejectReady;
  const ready=new Promise((resolve,reject)=>{resolveReady=resolve;rejectReady=reject});
  const apply=async action=>{
    const data=action.data;
    switch(action.action){
      case 'list_data_overwrite':return manager.listDataOverwrite(data);
      case 'list_music_add':return manager.listMusicAdd(data.id,data.musicInfos,data.addMusicLocationType);
      case 'list_music_remove':return manager.listMusicRemove(data.listId,data.ids);
      case 'list_create':for(const info of data.listInfos)await manager.userListCreate({...info,position:data.position});return;
      case 'list_remove':return manager.userListsRemove(data);
      case 'list_update':return manager.userListsUpdate(data);
      case 'list_update_position':return manager.userListsUpdatePosition(data.position,data.ids);
      case 'list_music_move':return manager.listMusicMove(data.fromId,data.toId,data.musicInfos,data.addMusicLocationType);
      case 'list_music_update':return manager.listMusicUpdateInfo(data);
      case 'list_music_update_position':return manager.listMusicUpdatePosition(data.listId,data.position,data.ids);
      case 'list_music_overwrite':return manager.listMusicOverwrite(data.listId,data.musicInfos);
      case 'list_music_clear':return manager.listMusicClear(data);
      default:throw Error('Fixture peer action not supported');
    }
  };
  const rpc=createMsg2call({
    funcsObj:{
      getEnabledFeatures:async(_type,versions)=>versions.list===1?{list:{skipSnapshot:false}}:{},
      list_sync_get_md5:async()=>createHash('md5').update(JSON.stringify(await manager.getListData())).digest('hex'),
      list_sync_get_list_data:async()=>manager.getListData(),
      list_sync_set_list_data:async data=>{await manager.listDataOverwrite(data)},
      list_sync_get_sync_mode:async()=> 'merge_local_remote',
      list_sync_finished:async()=>{listReady=true},
      finished:async()=>{if(!listReady)throw Error('Fixture list not ready');resolveReady()},
      onListSyncAction:async action=>{if(!listReady)throw Error('Fixture list not ready');await apply(action)},
    },
    sendMessage:message=>socket.send(encode(message)), timeout:15_000,
  });
  socket.on('message',data=>{const text=data.toString();if(text!=='ping')rpc.message(decode(text))});
  socket.on('error',()=>rejectReady(Error('Fixture socket failed')));
  socket.on('close',()=>{rpc.destroy();rejectReady(Error('Fixture socket closed'))});
  await Promise.race([ready,new Promise((_,reject)=>setTimeout(()=>reject(Error('Fixture ready timeout')),20_000))]);
  return {manager,rpc,apply,close(){rpc.destroy();socket.terminate()}};
}

const admin=http.createServer(async(request,response)=>{
  try{
    let body='';for await(const chunk of request){body+=chunk;if(body.length>65536)throw Error('Fixture body too large')}
    if(request.url==='/connect'){peer?.close();peer=await makePeer()}
    else if(request.url==='/add'){
      if(!peer)throw Error('Fixture peer not connected');
      const id=JSON.parse(body).id;
      const action={action:'list_music_add',data:{id:'default',musicInfos:[{id,name:'Fixture',singer:'Fixture',
        source:'kw',interval:null,meta:{songId:id,albumName:'',future:true}}],addMusicLocationType:'bottom'}};
      await peer.apply(action);await peer.rpc.remote.onListSyncAction(action);
    }
    else if(request.url==='/drop'){for(const socket of proxyClients)socket.terminate()}
    else if(request.url==='/drop-reply'){dropNextReply=true}
    else if(request.url==='/stats'){response.setHeader('Content-Type','application/json');response.end(JSON.stringify(stats));return}
    else if(request.url!=='/state'){response.writeHead(404).end();return}
    response.setHeader('Content-Type','application/json');response.end(JSON.stringify(peer?await peer.manager.getListData():empty()));
  }catch{response.writeHead(500).end('Fixture operation failed')}
});
const certDirectory=path.resolve('.contract-upstream/fixture-tls');fs.mkdirSync(certDirectory,{recursive:true});
const privatePath=path.join(certDirectory,'public-test-key.pem'),certificatePath=path.join(certDirectory,'public-test-cert.pem');
if(!fs.existsSync(privatePath))execFileSync('openssl',['req','-x509','-newkey','rsa:2048','-nodes',
  '-keyout',privatePath,'-out',certificatePath,'-days','2','-subj','/CN=public-fixture.invalid'],{stdio:'ignore'});
const tls=https.createServer({key:fs.readFileSync(privatePath),cert:fs.readFileSync(certificatePath)},(_request,response)=>response.end('not-trusted'));
tls.on('tlsClientError',()=>{});
proxy.listen(19528,'127.0.0.1');admin.listen(19529,'127.0.0.1');tls.listen(19530,'127.0.0.1');
console.log('Fixture reverse proxy, native-peer control and untrusted TLS endpoint ready');
const shutdown=()=>{peer?.close();sockets.close();proxy.close();admin.close();tls.close();process.exit(0)};
process.on('SIGTERM',shutdown);process.on('SIGINT',shutdown);
