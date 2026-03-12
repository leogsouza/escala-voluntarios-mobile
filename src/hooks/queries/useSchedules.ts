import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@/lib/queryKeys';
import { getActiveSchedules, getScheduleById } from '@/services/schedules';

export function useActiveSchedules() {
  return useQuery({
    queryKey: queryKeys.schedules.active(),
    queryFn: getActiveSchedules,
  });
}

export function useSchedule(id: number) {
  return useQuery({
    queryKey: queryKeys.schedules.detail(id),
    queryFn: () => getScheduleById(id),
    enabled: !!id,
  });
}
