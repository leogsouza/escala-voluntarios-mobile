import { apiRequest } from '@/services/api';
import { RestrictionType } from '@/types/restriction';

export async function getRestrictionTypes(): Promise<RestrictionType[]> {
  return apiRequest<RestrictionType[]>('/restriction-types');
}
