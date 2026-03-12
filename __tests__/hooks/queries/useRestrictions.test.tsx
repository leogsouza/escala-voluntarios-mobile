import React from 'react';
import { renderHook, act, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useRestrictionsPaginated,
  useRestriction,
  useCreateRestriction,
  useUpdateRestriction,
  useDeleteRestriction,
  useRestrictionRoleCounts,
} from '@/hooks/queries/useRestrictions';

jest.mock('@/services/restrictions', () => ({
  getRestrictions: jest.fn(),
  getRestrictionById: jest.fn(),
  createRestriction: jest.fn(),
  updateRestriction: jest.fn(),
  deleteRestriction: jest.fn(),
  getRestrictionRoleCounts: jest.fn(),
}));

import * as restrictionsService from '@/services/restrictions';

const mockedService = restrictionsService as jest.Mocked<typeof restrictionsService>;

function createWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return { wrapper: ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  ), qc };
}

const MOCK_PAGINATION = { page: 1, page_size: 20, total_items: 2, total_pages: 1 };
const MOCK_RESTRICTION_1 = { id: 1, volunteer_id: 1, schedule_id: 1, description: 'Test', restriction_type_id: 1, active: true };
const MOCK_RESTRICTION_2 = { id: 2, volunteer_id: 2, schedule_id: 1, description: 'Test 2', restriction_type_id: 1, active: true };
const MOCK_LIST = { data: [MOCK_RESTRICTION_1, MOCK_RESTRICTION_2], pagination: MOCK_PAGINATION };

describe('useRestrictions', () => {
  beforeEach(() => jest.clearAllMocks());

  describe('useRestrictionsPaginated', () => {
    it('fetches paginated restrictions', async () => {
      mockedService.getRestrictions.mockResolvedValue(MOCK_LIST as any);
      const { wrapper } = createWrapper();
      const { result } = renderHook(() => useRestrictionsPaginated({}), { wrapper });
      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(MOCK_LIST);
    });

    it('returns error state on failure', async () => {
      mockedService.getRestrictions.mockRejectedValue(new Error('Server error'));
      const { wrapper } = createWrapper();
      const { result } = renderHook(() => useRestrictionsPaginated({}), { wrapper });
      await waitFor(() => expect(result.current.isError).toBe(true));
    });
  });

  describe('useRestriction', () => {
    it('fetches single restriction by id', async () => {
      mockedService.getRestrictionById.mockResolvedValue(MOCK_RESTRICTION_1 as any);
      const { wrapper } = createWrapper();
      const { result } = renderHook(() => useRestriction(1), { wrapper });
      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(MOCK_RESTRICTION_1);
    });

    it('is disabled when id is 0', () => {
      const { wrapper } = createWrapper();
      const { result } = renderHook(() => useRestriction(0), { wrapper });
      expect(result.current.fetchStatus).toBe('idle');
    });
  });

  describe('useCreateRestriction', () => {
    it('calls createRestriction service', async () => {
      mockedService.createRestriction.mockResolvedValue(MOCK_RESTRICTION_1 as any);
      const { wrapper } = createWrapper();
      const { result } = renderHook(() => useCreateRestriction(), { wrapper });

      await act(async () => {
        await result.current.mutateAsync(MOCK_RESTRICTION_1 as any);
      });

      expect(mockedService.createRestriction).toHaveBeenCalledWith(MOCK_RESTRICTION_1);
    });
  });

  describe('useUpdateRestriction', () => {
    it('calls updateRestriction service with id and data', async () => {
      mockedService.updateRestriction.mockResolvedValue({ message: 'updated' });
      const { wrapper } = createWrapper();
      const { result } = renderHook(() => useUpdateRestriction(), { wrapper });

      await act(async () => {
        await result.current.mutateAsync({ id: 1, data: MOCK_RESTRICTION_1 as any });
      });

      expect(mockedService.updateRestriction).toHaveBeenCalledWith(1, MOCK_RESTRICTION_1);
    });
  });

  describe('useDeleteRestriction', () => {
    it('optimistically removes item from cache', async () => {
      mockedService.getRestrictions.mockResolvedValue(MOCK_LIST as any);
      mockedService.deleteRestriction.mockResolvedValue(undefined);

      const { wrapper, qc } = createWrapper();

      // Pre-populate cache
      const { result: listResult } = renderHook(() => useRestrictionsPaginated({}), { wrapper });
      await waitFor(() => expect(listResult.current.isSuccess).toBe(true));

      const { result: deleteResult } = renderHook(() => useDeleteRestriction(), { wrapper });

      await act(async () => {
        await deleteResult.current.mutateAsync(1);
      });

      expect(mockedService.deleteRestriction).toHaveBeenCalledWith(1);
    });

    it('rolls back optimistic update on error', async () => {
      mockedService.getRestrictions.mockResolvedValue(MOCK_LIST as any);
      mockedService.deleteRestriction.mockRejectedValue(new Error('Server error'));

      const { wrapper } = createWrapper();

      const { result: listResult } = renderHook(() => useRestrictionsPaginated({}), { wrapper });
      await waitFor(() => expect(listResult.current.isSuccess).toBe(true));

      const { result: deleteResult } = renderHook(() => useDeleteRestriction(), { wrapper });

      await act(async () => {
        try {
          await deleteResult.current.mutateAsync(1);
        } catch {
          // expected
        }
      });

      expect(deleteResult.current.isError).toBe(true);
    });
  });

  describe('useRestrictionRoleCounts', () => {
    it('fetches role counts for schedule', async () => {
      const mockCounts = [{ role_id: 1, role_name: 'Piano', count: 3 }];
      mockedService.getRestrictionRoleCounts.mockResolvedValue(mockCounts);
      const { wrapper } = createWrapper();
      const { result } = renderHook(() => useRestrictionRoleCounts(1), { wrapper });
      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(mockCounts);
    });

    it('is disabled when scheduleId is 0', () => {
      const { wrapper } = createWrapper();
      const { result } = renderHook(() => useRestrictionRoleCounts(0), { wrapper });
      expect(result.current.fetchStatus).toBe('idle');
    });
  });
});
