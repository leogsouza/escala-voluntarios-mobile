import { apiRequest } from '@/services/api';
import { searchVolunteers } from '@/services/volunteers';

jest.mock('@/services/api', () => ({
  apiRequest: jest.fn(),
}));

describe('services/volunteers', () => {
  const apiRequestMock = apiRequest as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('searchVolunteers encodes and calls search endpoint', async () => {
    apiRequestMock.mockResolvedValueOnce([{ id: 4, name: 'João' }]);

    const result = await searchVolunteers('jo ao');

    expect(apiRequestMock).toHaveBeenCalledWith('/volunteers/search?q=jo%20ao');
    expect(result).toEqual([{ id: 4, name: 'João' }]);
  });
});
