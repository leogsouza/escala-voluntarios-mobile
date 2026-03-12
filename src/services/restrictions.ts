import { apiRequest } from '@/services/api';
import {
  PaginatedResponse,
  Restriction,
} from '@/types/restriction';

export interface RestrictionQueryParams {
  page?: number;
  page_size?: number;
  schedule_id?: number;
  volunteer_id?: number;
  role_ids?: number[];
  q?: string;
  active_only?: boolean;
}

export interface RestrictionRoleCount {
  role_id: number;
  role_name: string;
  count: number;
}

export async function getRestrictions(
  params: RestrictionQueryParams = {},
): Promise<PaginatedResponse<Restriction>> {
  const searchParams = new URLSearchParams();

  if (typeof params.page === 'number') searchParams.set('page', String(params.page));
  if (typeof params.page_size === 'number') searchParams.set('page_size', String(params.page_size));
  if (typeof params.schedule_id === 'number') searchParams.set('schedule_id', String(params.schedule_id));
  if (typeof params.volunteer_id === 'number') searchParams.set('volunteer_id', String(params.volunteer_id));
  if (params.role_ids && params.role_ids.length > 0) searchParams.set('role_ids', params.role_ids.join(','));
  if (params.q && params.q.trim()) searchParams.set('q', params.q.trim());
  if (params.active_only) searchParams.set('active_only', 'true');

  const query = searchParams.toString();
  return apiRequest<PaginatedResponse<Restriction>>(`/restrictions${query ? `?${query}` : ''}`);
}

export async function getRestrictionById(id: number): Promise<Restriction> {
  return apiRequest<Restriction>(`/restrictions/${id}`);
}

export async function createRestriction(data: Restriction): Promise<Restriction> {
  return apiRequest<Restriction>('/restrictions', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateRestriction(id: number, data: Restriction): Promise<{ message: string }> {
  return apiRequest<{ message: string }>(`/restrictions/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteRestriction(id: number): Promise<void> {
  await apiRequest<void>(`/restrictions/${id}`, {
    method: 'DELETE',
  });
}

export async function getRestrictionRoleCounts(scheduleId: number): Promise<RestrictionRoleCount[]> {
  return apiRequest<RestrictionRoleCount[]>(`/restrictions/role-counts?schedule_id=${scheduleId}`);
}
