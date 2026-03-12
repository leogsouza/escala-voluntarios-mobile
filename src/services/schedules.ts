import { apiRequest } from '@/services/api';
import { Schedule } from '@/types/schedule';

export async function getActiveSchedules(): Promise<Schedule[]> {
  return apiRequest<Schedule[]>('/schedules/active');
}

export async function getScheduleById(id: number): Promise<Schedule> {
  return apiRequest<Schedule>(`/schedules/${id}`);
}
