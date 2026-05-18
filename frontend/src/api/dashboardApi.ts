import {http} from './http'; export const getMonthlyDashboard=(pid:string,year:number,month:number)=>http.get(`/profiles/${pid}/dashboard/monthly?year=${year}&month=${month}`).then(r=>r.data);
