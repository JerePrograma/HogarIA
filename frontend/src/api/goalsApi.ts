import { http } from './http';
export const listGoals=(profileId:string)=>http.get(`/api/profiles/${profileId}/goals`).then(r=>r.data);
export const createGoal=(profileId:string,payload:any)=>http.post(`/api/profiles/${profileId}/goals`,payload).then(r=>r.data);
export const createEmergencyFund=(profileId:string,coverageMonths:number)=>http.post(`/api/profiles/${profileId}/goals/emergency-fund`,{coverageMonths}).then(r=>r.data);
export const deleteGoal=(profileId:string,goalId:string)=>http.delete(`/api/profiles/${profileId}/goals/${goalId}`).then(r=>r.data);
