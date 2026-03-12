import { apiRequest } from '@/services/api';
import { Volunteer } from '@/types/schedule';

export async function searchVolunteers(query: string): Promise<Volunteer[]> {
  return apiRequest<Volunteer[]>(`/volunteers/search?q=${encodeURIComponent(query)}`);
}
