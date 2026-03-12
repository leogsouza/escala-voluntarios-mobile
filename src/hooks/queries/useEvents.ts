import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@/lib/queryKeys';
import { getEventsByMonth } from '@/services/events';

export function useEventsByMonth(year: number, month: number, scheduleId?: number) {
  return useQuery({
    queryKey: queryKeys.events.byMonth(year, month, scheduleId),
    queryFn: () => getEventsByMonth(year, month, scheduleId),
    enabled: !!year && !!month,
  });
}
