export const todayYm = () => { const d = new Date(); return { year: d.getFullYear(), month: d.getMonth() + 1 }; };
