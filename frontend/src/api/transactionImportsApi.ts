import {http} from './http';

export const previewTransactionImport=(profileId:string,source:string,accountId:string,file:File)=>{const fd=new FormData();fd.append('file',file); return http.post(`/api/profiles/${profileId}/transaction-imports/preview?source=${source}&accountId=${accountId}`,fd,{headers:{'Content-Type':'multipart/form-data'}}).then(r=>r.data);};
export const commitTransactionImport=(profileId:string,batchId:string,payload:unknown)=>http.post(`/api/profiles/${profileId}/transaction-imports/${batchId}/commit`,payload).then(r=>r.data);
