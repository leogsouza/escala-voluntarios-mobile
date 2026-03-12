import { useQuery } from '@tanstack/react-query';

import { queryKeys } from '@/lib/queryKeys';
import { getRestrictionTypes } from '@/services/restriction-types';

export function useRestrictionTypes() {
  return useQuery({
    queryKey: queryKeys.restrictionTypes.all(),
    queryFn: getRestrictionTypes,
  });
}
