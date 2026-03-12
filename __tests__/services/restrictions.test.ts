import { apiRequest } from '@/services/api';
import {
  createRestriction,
  deleteRestriction,
  getRestrictionById,
  getRestrictionRoleCounts,
  getRestrictions,
  updateRestriction,
} from '@/services/restrictions';

jest.mock('@/services/api', () => ({
  apiRequest: jest.fn(),
}));

describe('services/restrictions', () => {
  const apiRequestMock = apiRequest as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('builds restrictions query params following backend names', async () => {
    apiRequestMock.mockResolvedValueOnce({ data: [], pagination: {} });

    await getRestrictions({
      page: 1,
      page_size: 25,
      schedule_id: 9,
      volunteer_id: 7,
      role_ids: [1, 2],
      q: ' leo ',
      active_only: true,
    });

    expect(apiRequestMock).toHaveBeenCalledWith(
      '/restrictions?page=1&page_size=25&schedule_id=9&volunteer_id=7&role_ids=1%2C2&q=leo&active_only=true',
    );
  });

  it('getRestrictionById requests /restrictions/:id', async () => {
    apiRequestMock.mockResolvedValueOnce({ id: 3 });

    const result = await getRestrictionById(3);

    expect(apiRequestMock).toHaveBeenCalledWith('/restrictions/3');
    expect(result).toEqual({ id: 3 });
  });

  it('createRestriction posts payload', async () => {
    const payload = {
      volunteer_id: 1,
      schedule_id: 2,
      description: 'x',
      restriction_type_id: 3,
      rules_json: '{"mode":"exclude"}',
      active: true,
    };
    apiRequestMock.mockResolvedValueOnce({ id: 8 });

    const result = await createRestriction(payload);

    expect(apiRequestMock).toHaveBeenCalledWith('/restrictions', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    expect(result).toEqual({ id: 8 });
  });

  it('updateRestriction puts payload', async () => {
    const payload = {
      volunteer_id: 1,
      schedule_id: 2,
      description: 'y',
      restriction_type_id: 3,
      rules_json: '{"mode":"include"}',
      active: false,
    };
    apiRequestMock.mockResolvedValueOnce({ message: 'ok' });

    const result = await updateRestriction(6, payload);

    expect(apiRequestMock).toHaveBeenCalledWith('/restrictions/6', {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
    expect(result).toEqual({ message: 'ok' });
  });

  it('deleteRestriction issues delete request', async () => {
    apiRequestMock.mockResolvedValueOnce(undefined);

    await deleteRestriction(11);

    expect(apiRequestMock).toHaveBeenCalledWith('/restrictions/11', {
      method: 'DELETE',
    });
  });

  it('getRestrictionRoleCounts requests schedule_id query', async () => {
    apiRequestMock.mockResolvedValueOnce([{ role_id: 1, role_name: 'Música', count: 9 }]);

    const result = await getRestrictionRoleCounts(33);

    expect(apiRequestMock).toHaveBeenCalledWith('/restrictions/role-counts?schedule_id=33');
    expect(result).toEqual([{ role_id: 1, role_name: 'Música', count: 9 }]);
  });
});
