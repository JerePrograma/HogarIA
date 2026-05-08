import { http } from './http';
export const listHabits=(profileId:string)=>http.get(`/api/profiles/${profileId}/habits`).then(r=>r.data);
export const createHabit=(profileId:string,payload:any)=>http.post(`/api/profiles/${profileId}/habits`,payload).then(r=>r.data);
export const upsertHabitCheckin=(profileId:string,habitId:string,date:string,done:boolean)=>http.put(`/api/profiles/${profileId}/habits/${habitId}/checkins/${date}`,{done}).then(r=>r.data);
