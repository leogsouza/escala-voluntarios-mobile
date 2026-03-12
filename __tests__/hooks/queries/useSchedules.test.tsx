import React from 'react';
import { renderHook, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useActiveSchedules, useSchedule } from '@/hooks/queries/useSchedules';

jest.mock('@/services/schedules', () => ({
  getActiveSchedules: jest.fn(),
  getScheduleById: jest.fn(),
}));

import * as schedulesService from '@/services/schedules';

const mockedService = schedulesService as jest.Mocked<typeof schedulesService>;

function createWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

const MOCK_SCHEDULES = [
  { id: 1, name: 'Março 2026', church_id: 1, status: 'published', start_date: '2026-03-01', end_date: '2026-03-31' },
];

describe('useSchedules', () => {
  beforeEach(() => jest.clearAllMocks());

  describe('useActiveSchedules', () => {
    it('starts in loading state', () => {
      mockedService.getActiveSchedules.mockResolvedValue(MOCK_SCHEDULES as any);
      const { result } = renderHook(() => useActiveSchedules(), { wrapper: createWrapper() });
      expect(result.current.isLoading).toBe(true);
    });

    it('returns schedules on success', async () => {
      mockedService.getActiveSchedules.mockResolvedValue(MOCK_SCHEDULES as any);
      const { result } = renderHook(() => useActiveSchedules(), { wrapper: createWrapper() });
      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(MOCK_SCHEDULES);
    });

    it('returns error on failure', async () => {
      mockedService.getActiveSchedules.mockRejectedValue(new Error('Network error'));
      const { result } = renderHook(() => useActiveSchedules(), { wrapper: createWrapper() });
      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(result.current.error).toBeDefined();
    });
  });

  describe('useSchedule', () => {
    it('fetches single schedule by id', async () => {
      mockedService.getScheduleById.mockResolvedValue(MOCK_SCHEDULES[0] as any);
      const { result } = renderHook(() => useSchedule(1), { wrapper: createWrapper() });
      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(MOCK_SCHEDULES[0]);
    });

    it('is disabled when id is 0', () => {
      const { result } = renderHook(() => useSchedule(0), { wrapper: createWrapper() });
      expect(result.current.fetchStatus).toBe('idle');
    });
  });
});
