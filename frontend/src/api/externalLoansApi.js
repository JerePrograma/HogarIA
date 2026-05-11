import { http } from './http';
export const getExternalLoansSummary = async (profileId) => {
    const response = await http.get(`/api/profiles/${profileId}/external-loans/summary`);
    return response.data;
};
export const getExternalLoanSyncConfig = async (profileId) => {
    const response = await http.get(`/api/profiles/${profileId}/external-loans/sync-config`);
    return response.data;
};
export const saveExternalLoanSyncConfig = async (profileId, payload) => {
    const response = await http.put(`/api/profiles/${profileId}/external-loans/sync-config`, payload);
    return response.data;
};
export const syncExternalLoans = async (profileId) => {
    const response = await http.post(`/api/profiles/${profileId}/external-loans/sync`);
    return response.data;
};
