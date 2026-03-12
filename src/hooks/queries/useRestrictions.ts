import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { queryKeys } from '@/lib/queryKeys';
import {
  createRestriction,
  deleteRestriction,
  getRestrictionById,
  getRestrictions,
  getRestrictionRoleCounts,
  updateRestriction,
  RestrictionQueryParams,
} from '@/services/restrictions';
import { Restriction, PaginatedResponse } from '@/types/restriction';

export function useRestrictionsPaginated(params: RestrictionQueryParams = {}) {
  return useQuery({
    queryKey: queryKeys.restrictions.list(params),
    queryFn: () => getRestrictions(params),
  });
}

export function useRestriction(id: number) {
  return useQuery({
    queryKey: queryKeys.restrictions.detail(id),
    queryFn: () => getRestrictionById(id),
    enabled: !!id,
  });
}

export function useCreateRestriction() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Restriction) => createRestriction(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.restrictions.all() });
    },
  });
}

export function useUpdateRestriction() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Restriction }) => updateRestriction(id, data),
    onSuccess: (_result, { id }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.restrictions.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.restrictions.detail(id) });
    },
  });
}

export function useDeleteRestriction() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteRestriction(id),
    onMutate: async (id) => {
      // Cancel in-flight queries
      await queryClient.cancelQueries({ queryKey: queryKeys.restrictions.all() });

      // Snapshot previous data from all matching queries
      const previousQueries = queryClient.getQueriesData<PaginatedResponse<Restriction>>({
        queryKey: queryKeys.restrictions.all(),
      });

      // Optimistically remove from all list caches
      queryClient.setQueriesData<PaginatedResponse<Restriction>>(
        { queryKey: queryKeys.restrictions.all() },
        (old) => {
          if (!old?.data) return old;
          return {
            ...old,
            data: old.data.filter((r) => r.id !== id),
            pagination: {
              ...old.pagination,
              total_items: old.pagination.total_items - 1,
            },
          };
        },
      );

      return { previousQueries };
    },
    onError: (_err, _id, context) => {
      // Rollback optimistic update
      if (context?.previousQueries) {
        for (const [queryKey, data] of context.previousQueries) {
          queryClient.setQueryData(queryKey, data);
        }
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.restrictions.all() });
    },
  });
}

export function useRestrictionRoleCounts(scheduleId: number) {
  return useQuery({
    queryKey: queryKeys.restrictions.roleCounts(scheduleId),
    queryFn: () => getRestrictionRoleCounts(scheduleId),
    enabled: !!scheduleId,
  });
}
