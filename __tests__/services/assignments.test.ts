import { apiRequest } from '@/services/api';
import { getAssignmentsByEvent, getPublishedAssignments } from '@/services/assignments';

jest.mock('@/services/api', () => ({
  apiRequest: jest.fn(),
}));

describe('services/assignments', () => {
  const apiRequestMock = apiRequest as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('getPublishedAssignments requests schedule published endpoint', async () => {
    apiRequestMock.mockResolvedValueOnce([{ id: 10 }]);

    const result = await getPublishedAssignments(12);

    expect(apiRequestMock).toHaveBeenCalledWith('/schedules/12/assignments/published');
    expect(result).toEqual([{ id: 10 }]);
  });

  it('getAssignmentsByEvent requests event endpoint', async () => {
    apiRequestMock.mockResolvedValueOnce([{ id: 11 }]);

    const result = await getAssignmentsByEvent(45);

    expect(apiRequestMock).toHaveBeenCalledWith('/assignments/event/45');
    expect(result).toEqual([{ id: 11 }]);
  });
});
