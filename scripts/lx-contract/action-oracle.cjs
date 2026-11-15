// Test-only: execute the original compiled ListDataManage from the pinned deployment copy.
const path=require('node:path');
const {createHash}=require('node:crypto');
const {ListDataManage}=require(path.resolve('.contract-upstream/lx-deployment/server/modules/list/listDataManage.js'));
const hash=value=>createHash('md5').update(value).digest('hex');
async function run(input){
 Date.now=()=>input.now;
 const manager=new ListDataManage({getSnapshotInfo:async()=>({latest:'fixture'}),getSnapshot:async()=>structuredClone(input.initial)});
 await new Promise(setImmediate);
 const snapshots=[];
 for(const {action,data} of input.actions){
  switch(action){
   case 'list_data_overwrite':await manager.listDataOverwrite(data);break;
   case 'list_create':for(const info of data.listInfos)await manager.userListCreate({...info,position:data.position});break;
   case 'list_remove':await manager.userListsRemove(data);break;
   case 'list_update':await manager.userListsUpdate(data);break;
   case 'list_update_position':await manager.userListsUpdatePosition(data.position,data.ids);break;
   case 'list_music_add':await manager.listMusicAdd(data.id,data.musicInfos,data.addMusicLocationType);break;
   case 'list_music_move':await manager.listMusicMove(data.fromId,data.toId,data.musicInfos,data.addMusicLocationType);break;
   case 'list_music_remove':await manager.listMusicRemove(data.listId,data.ids);break;
   case 'list_music_update':await manager.listMusicUpdateInfo(data);break;
   case 'list_music_update_position':await manager.listMusicUpdatePosition(data.listId,data.position,data.ids);break;
   case 'list_music_overwrite':await manager.listMusicOverwrite(data.listId,data.musicInfos);break;
   case 'list_music_clear':await manager.listMusicClear(data);break;
   default:throw Error('Unknown fixture action');
  }
  const canonical=JSON.stringify(await manager.getListData());snapshots.push({canonical,md5:hash(canonical)});
 }
 return snapshots;
}
let text='';process.stdin.setEncoding('utf8');process.stdin.on('data',chunk=>{text+=chunk});
process.stdin.on('end',()=>run(JSON.parse(text)).then(result=>process.stdout.write(JSON.stringify(result))).catch(error=>{console.error(error);process.exitCode=1}));
