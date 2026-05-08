import { http } from './http';
export const listInflation=(year:number)=>http.get(`/api/inflation?year=${year}`).then(r=>r.data);
export const createInflation=(payload:any)=>http.post('/api/inflation',payload).then(r=>r.data);
export const getAccumulatedInflation=(fromYear:number,fromMonth:number,toYear:number,toMonth:number)=>http.get(`/api/inflation/accumulated?fromYear=${fromYear}&fromMonth=${fromMonth}&toYear=${toYear}&toMonth=${toMonth}`).then(r=>r.data);
