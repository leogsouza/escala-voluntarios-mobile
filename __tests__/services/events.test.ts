import { apiRequest } from '@/services/api';
import { getEventsByMonth } from '@/services/events';

jest.mock('@/services/api', () => ({
  apiRequest: jest.fn(),
}));

describe('services/events', () => {
  const apiRequestMock = apiRequest as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('requests monthly events without schedule filter', async () => {
    apiRequestMock.mockResolvedValueOnce([{ id: 1 }]);

    const result = await getEventsByMonth(2026, 3);

    expect(apiRequestMock).toHaveBeenCalledWith('/events/month/2026/3');
    expect(result).toEqual([{ id: 1 }]);
  });

  it('requests monthly events with schedule_id query param', async () => {
    apiRequestMock.mockResolvedValueOnce([{ id: 2 }]);

    const result = await getEventsByMonth(2026, 3, 77);

    expect(apiRequestMock).toHaveBeenCalledWith('/events/month/2026/3?schedule_id=77');
    expect(result).toEqual([{ id: 2 }]);
  });
});
