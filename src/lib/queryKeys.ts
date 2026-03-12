import type { RestrictionQueryParams } from '@/services/restrictions';

export const queryKeys = {
  schedules: {
    all: () => ['schedules'] as const,
    active: () => ['schedules', 'active'] as const,
    detail: (id: number) => ['schedules', id] as const,
  },
  events: {
    all: () => ['events'] as const,
    byMonth: (year: number, month: number, scheduleId?: number) =>
      ['events', 'month', year, month, scheduleId] as const,
  },
  assignments: {
    byEvent: (eventId: number) => ['assignments', 'event', eventId] as const,
    published: (scheduleId: number) => ['assignments', 'published', scheduleId] as const,
  },
  restrictions: {
    all: () => ['restrictions'] as const,
    list: (params: RestrictionQueryParams) => ['restrictions', 'list', params] as const,
    detail: (id: number) => ['restrictions', id] as const,
    roleCounts: (scheduleId: number) => ['restrictions', 'role-counts', scheduleId] as const,
  },
  restrictionTypes: {
    all: () => ['restriction-types'] as const,
  },
  volunteers: {
    search: (query: string) => ['volunteers', 'search', query] as const,
  },
};
