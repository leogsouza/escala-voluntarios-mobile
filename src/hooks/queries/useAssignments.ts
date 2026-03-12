import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@/lib/queryKeys';
import { getPublishedAssignments, getAssignmentsByEvent } from '@/services/assignments';

export function usePublishedAssignments(scheduleId: number) {
  return useQuery({
    queryKey: queryKeys.assignments.published(scheduleId),
    queryFn: () => getPublishedAssignments(scheduleId),
    enabled: !!scheduleId,
  });
}

export function useAssignmentsByEvent(eventId: number) {
  return useQuery({
    queryKey: queryKeys.assignments.byEvent(eventId),
    queryFn: () => getAssignmentsByEvent(eventId),
    enabled: !!eventId,
  });
}
