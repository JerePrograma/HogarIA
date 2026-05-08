import { http } from './http';
export const listInflation = (year) => http.get(`/api/inflation?year=${year}`).then(r => r.data);
export const createInflation = (payload) => http.post('/api/inflation', payload).then(r => r.data);
export const getAccumulatedInflation = (fromYear, fromMonth, toYear, toMonth) => http.get(`/api/inflation/accumulated?fromYear=${fromYear}&fromMonth=${fromMonth}&toYear=${toYear}&toMonth=${toMonth}`).then(r => r.data);
