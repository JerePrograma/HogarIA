import { http } from './http';
export const listGoals=(profileId:string)=>http.get(`/profiles/${profileId}/goals`).then(r=>r.data);
export const createGoal=(profileId:string,payload:any)=>http.post(`/profiles/${profileId}/goals`,payload).then(r=>r.data);
export const createEmergencyFund=(profileId:string,coverageMonths:number)=>http.post(`/profiles/${profileId}/goals/emergency-fund`,{coverageMonths}).then(r=>r.data);
export const deleteGoal=(profileId:string,goalId:string)=>http.delete(`/profiles/${profileId}/goals/${goalId}`).then(r=>r.data);
