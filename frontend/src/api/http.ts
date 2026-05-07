import axios from 'axios';
export const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080' });
http.interceptors.request.use((config) => { const id = localStorage.getItem('devUserId'); if (id) config.headers['X-User-Id'] = id; return config; });
