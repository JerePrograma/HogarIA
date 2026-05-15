import { useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '../domain/queryKeys';

export function useInvalidateMonthlyViews(profileId: string, year: number, month: number) {
  const queryClient = useQueryClient();

  return () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.planning(profileId, year, month) });
    queryClient.invalidateQueries({ queryKey: queryKeys.dashboard(profileId, year, month) });
    queryClient.invalidateQueries({ queryKey: queryKeys.transactions(profileId, year, month) });
  };
}
