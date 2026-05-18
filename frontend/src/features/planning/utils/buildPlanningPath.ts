export function buildPlanningPath(path: string, params: URLSearchParams): string {
  const q = params.toString();
  return q ? `${path}?${q}` : path;
}
