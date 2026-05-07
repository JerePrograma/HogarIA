import {http} from './http'; export const getMonthlyDashboard=(pid:string,year:number,month:number)=>http.get(`/api/profiles/${pid}/dashboard/monthly?year=${year}&month=${month}`).then(r=>r.data);
