import { http } from './http';
export async function previewBudgetExcelImport(profileId, file) {
    const form = new FormData();
    form.append('file', file);
    const { data } = await http.post(`/api/profiles/${profileId}/imports/budget-excel/preview`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
}
export async function commitBudgetExcelImport(profileId, batchId, body) {
    const { data } = await http.post(`/api/profiles/${profileId}/imports/budget-excel/${batchId}/commit`, body);
    return data;
}
export async function getImportBatch(profileId, batchId) {
    const { data } = await http.get(`/api/profiles/${profileId}/imports/${batchId}`);
    return data;
}
export async function listImportBatches(profileId) {
    const { data } = await http.get(`/api/profiles/${profileId}/imports`);
    return data;
}
