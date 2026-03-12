import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@/lib/queryKeys';
import { searchVolunteers } from '@/services/volunteers';

export function useSearchVolunteers(query: string) {
  return useQuery({
    queryKey: queryKeys.volunteers.search(query),
    queryFn: () => searchVolunteers(query),
    enabled: query.trim().length >= 2,
  });
}
