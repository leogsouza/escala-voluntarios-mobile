import { apiRequest } from '@/services/api';
import { Assignment } from '@/types/schedule';

export async function getPublishedAssignments(scheduleId: number): Promise<Assignment[]> {
  return apiRequest<Assignment[]>(`/schedules/${scheduleId}/assignments/published`);
}

export async function getAssignmentsByEvent(eventId: number): Promise<Assignment[]> {
  return apiRequest<Assignment[]>(`/assignments/event/${eventId}`);
}
