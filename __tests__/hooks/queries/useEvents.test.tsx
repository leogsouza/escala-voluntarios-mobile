import React from 'react';
import { renderHook, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useEventsByMonth } from '@/hooks/queries/useEvents';

jest.mock('@/services/events', () => ({
  getEventsByMonth: jest.fn(),
}));

import * as eventsService from '@/services/events';

const mockedService = eventsService as jest.Mocked<typeof eventsService>;

function createWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

const MOCK_EVENTS = [
  { id: 1, date: '2026-03-05', schedule_id: 1, template_id: 1, service_code_id: 1 },
  { id: 2, date: '2026-03-12', schedule_id: 1, template_id: 1, service_code_id: 2 },
];

describe('useEvents', () => {
  beforeEach(() => jest.clearAllMocks());

  describe('useEventsByMonth', () => {
    it('starts in loading state', () => {
      mockedService.getEventsByMonth.mockResolvedValue(MOCK_EVENTS as any);
      const { result } = renderHook(() => useEventsByMonth(2026, 3), { wrapper: createWrapper() });
      expect(result.current.isLoading).toBe(true);
    });

    it('returns events for month', async () => {
      mockedService.getEventsByMonth.mockResolvedValue(MOCK_EVENTS as any);
      const { result } = renderHook(() => useEventsByMonth(2026, 3), { wrapper: createWrapper() });
      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(MOCK_EVENTS);
    });

    it('passes scheduleId to service when provided', async () => {
      mockedService.getEventsByMonth.mockResolvedValue(MOCK_EVENTS as any);
      const { result } = renderHook(() => useEventsByMonth(2026, 3, 42), { wrapper: createWrapper() });
      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(mockedService.getEventsByMonth).toHaveBeenCalledWith(2026, 3, 42);
    });

    it('returns error state on failure', async () => {
      mockedService.getEventsByMonth.mockRejectedValue(new Error('Not found'));
      const { result } = renderHook(() => useEventsByMonth(2026, 3), { wrapper: createWrapper() });
      await waitFor(() => expect(result.current.isError).toBe(true));
    });

    it('does NOT expose any mutation hooks (read-only)', () => {
      // useEvents module must not export useMutation-based functions
      const module = require('@/hooks/queries/useEvents');
      const keys = Object.keys(module);
      expect(keys.every((k) => !k.startsWith('useCreate') && !k.startsWith('useUpdate') && !k.startsWith('useDelete'))).toBe(true);
    });
  });
});
