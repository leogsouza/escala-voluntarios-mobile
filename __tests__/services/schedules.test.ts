import { apiRequest } from '@/services/api';
import { getActiveSchedules, getScheduleById } from '@/services/schedules';

jest.mock('@/services/api', () => ({
  apiRequest: jest.fn(),
}));

describe('services/schedules', () => {
  const apiRequestMock = apiRequest as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('getActiveSchedules calls /schedules/active', async () => {
    apiRequestMock.mockResolvedValueOnce([{ id: 1 }]);

    const result = await getActiveSchedules();

    expect(apiRequestMock).toHaveBeenCalledWith('/schedules/active');
    expect(result).toEqual([{ id: 1 }]);
  });

  it('getScheduleById calls /schedules/:id', async () => {
    apiRequestMock.mockResolvedValueOnce({ id: 9 });

    const result = await getScheduleById(9);

    expect(apiRequestMock).toHaveBeenCalledWith('/schedules/9');
    expect(result).toEqual({ id: 9 });
  });
});
