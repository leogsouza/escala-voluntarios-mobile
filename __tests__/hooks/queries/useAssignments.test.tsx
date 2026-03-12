import React from 'react';
import { renderHook, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { usePublishedAssignments, useAssignmentsByEvent } from '@/hooks/queries/useAssignments';

jest.mock('@/services/assignments', () => ({
  getPublishedAssignments: jest.fn(),
  getAssignmentsByEvent: jest.fn(),
}));

import * as assignmentsService from '@/services/assignments';

const mockedService = assignmentsService as jest.Mocked<typeof assignmentsService>;

function createWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

const MOCK_ASSIGNMENTS = [
  { id: 1, event_id: 10, volunteer_id: 5, position_id: 3, status: 'published' },
];

describe('useAssignments', () => {
  beforeEach(() => jest.clearAllMocks());

  describe('usePublishedAssignments', () => {
    it('returns published assignments for schedule', async () => {
      mockedService.getPublishedAssignments.mockResolvedValue(MOCK_ASSIGNMENTS as any);
      const { result } = renderHook(() => usePublishedAssignments(1), { wrapper: createWrapper() });
      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(MOCK_ASSIGNMENTS);
    });

    it('is disabled when scheduleId is 0', () => {
      const { result } = renderHook(() => usePublishedAssignments(0), { wrapper: createWrapper() });
      expect(result.current.fetchStatus).toBe('idle');
    });

    it('returns error state on failure', async () => {
      mockedService.getPublishedAssignments.mockRejectedValue(new Error('Server error'));
      const { result } = renderHook(() => usePublishedAssignments(1), { wrapper: createWrapper() });
      await waitFor(() => expect(result.current.isError).toBe(true));
    });
  });

  describe('useAssignmentsByEvent', () => {
    it('returns assignments for event', async () => {
      mockedService.getAssignmentsByEvent.mockResolvedValue(MOCK_ASSIGNMENTS as any);
      const { result } = renderHook(() => useAssignmentsByEvent(10), { wrapper: createWrapper() });
      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(MOCK_ASSIGNMENTS);
    });

    it('is disabled when eventId is 0', () => {
      const { result } = renderHook(() => useAssignmentsByEvent(0), { wrapper: createWrapper() });
      expect(result.current.fetchStatus).toBe('idle');
    });
  });

  it('does NOT expose any mutation hooks (read-only)', () => {
    const module = require('@/hooks/queries/useAssignments');
    const keys = Object.keys(module);
    expect(keys.every((k) => !k.startsWith('useCreate') && !k.startsWith('useUpdate') && !k.startsWith('useDelete'))).toBe(true);
  });
});
