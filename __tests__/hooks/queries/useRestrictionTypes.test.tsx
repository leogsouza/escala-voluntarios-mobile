import React from 'react';
import { renderHook, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useRestrictionTypes } from '@/hooks/queries/useRestrictionTypes';

jest.mock('@/services/restriction-types', () => ({
  getRestrictionTypes: jest.fn(),
}));

import * as service from '@/services/restriction-types';

const mockedService = service as jest.Mocked<typeof service>;

function createWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

describe('useRestrictionTypes', () => {
  beforeEach(() => jest.clearAllMocks());

  it('returns restriction types on success', async () => {
    const mockTypes = [{ id: 1, name: 'Folga' }, { id: 2, name: 'Férias' }];
    mockedService.getRestrictionTypes.mockResolvedValue(mockTypes);
    const { result } = renderHook(() => useRestrictionTypes(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(mockTypes);
  });

  it('returns error state on failure', async () => {
    mockedService.getRestrictionTypes.mockRejectedValue(new Error('Not found'));
    const { result } = renderHook(() => useRestrictionTypes(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
