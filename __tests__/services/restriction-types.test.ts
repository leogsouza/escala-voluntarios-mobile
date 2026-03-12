import { apiRequest } from '@/services/api';
import { getRestrictionTypes } from '@/services/restriction-types';

jest.mock('@/services/api', () => ({
  apiRequest: jest.fn(),
}));

describe('services/restriction-types', () => {
  const apiRequestMock = apiRequest as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('requests /restriction-types', async () => {
    apiRequestMock.mockResolvedValueOnce([{ id: 1, name: 'TRAVEL' }]);

    const result = await getRestrictionTypes();

    expect(apiRequestMock).toHaveBeenCalledWith('/restriction-types');
    expect(result).toEqual([{ id: 1, name: 'TRAVEL' }]);
  });
});
