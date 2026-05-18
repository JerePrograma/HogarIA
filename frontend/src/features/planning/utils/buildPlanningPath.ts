export function buildPlanningPath(path: string, params: URLSearchParams): string {
  const query = params.toString();
  if (!query) return path;
  const hasQuery = path.includes('?');
  return `${path}${hasQuery ? '&' : '?'}${query}`;
}
