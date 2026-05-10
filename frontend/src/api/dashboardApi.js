import { http } from './http';
export const getMonthlyDashboard = (pid, year, month) => http.get(`/api/profiles/${pid}/dashboard/monthly?year=${year}&month=${month}`).then(r => r.data);
