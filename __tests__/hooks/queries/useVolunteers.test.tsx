import React from 'react';
import { renderHook, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useSearchVolunteers } from '@/hooks/queries/useVolunteers';

jest.mock('@/services/volunteers', () => ({
  searchVolunteers: jest.fn(),
}));

import * as volunteersService from '@/services/volunteers';

const mockedService = volunteersService as jest.Mocked<typeof volunteersService>;

function createWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

const MOCK_VOLUNTEERS = [
  { id: 1, name: 'João Silva', email: 'joao@test.com', active: true },
];

describe('useSearchVolunteers', () => {
  beforeEach(() => jest.clearAllMocks());

  it('is disabled when query is less than 2 chars', () => {
    const { result } = renderHook(() => useSearchVolunteers('a'), { wrapper: createWrapper() });
    expect(result.current.fetchStatus).toBe('idle');
  });

  it('is disabled when query is empty', () => {
    const { result } = renderHook(() => useSearchVolunteers(''), { wrapper: createWrapper() });
    expect(result.current.fetchStatus).toBe('idle');
  });

  it('fetches when query is 2+ chars', async () => {
    mockedService.searchVolunteers.mockResolvedValue(MOCK_VOLUNTEERS as any);
    const { result } = renderHook(() => useSearchVolunteers('Jo'), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(MOCK_VOLUNTEERS);
  });

  it('calls searchVolunteers with the query string', async () => {
    mockedService.searchVolunteers.mockResolvedValue(MOCK_VOLUNTEERS as any);
    renderHook(() => useSearchVolunteers('João'), { wrapper: createWrapper() });
    await waitFor(() => expect(mockedService.searchVolunteers).toHaveBeenCalledWith('João'));
  });

  it('returns error state on failure', async () => {
    mockedService.searchVolunteers.mockRejectedValue(new Error('Server error'));
    const { result } = renderHook(() => useSearchVolunteers('Jo'), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
