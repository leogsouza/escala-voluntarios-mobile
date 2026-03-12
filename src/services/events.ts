import { apiRequest } from '@/services/api';
import { Event } from '@/types/schedule';

export async function getEventsByMonth(
  year: number,
  month: number,
  scheduleId?: number,
): Promise<Event[]> {
  const query = typeof scheduleId === 'number' ? `?schedule_id=${scheduleId}` : '';
  return apiRequest<Event[]>(`/events/month/${year}/${month}${query}`);
}
