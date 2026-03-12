import { Schedule, Volunteer } from './schedule';

export type RestrictionType = {
  id: number;
  name: string;
  description?: string;
};

export interface SpecificDate {
  date: string;
  positionID?: number | null;
  notes?: string;
}

export interface DateRangeEntry {
  start: string;
  end: string;
  positionID?: number | null;
}

export interface RestrictionRules {
  mode: 'exclude' | 'include';
  operator?: 'OR' | 'AND';
  weekdays?: number[];
  periods?: string[];
  serviceCodes?: string[];
  specificDates?: SpecificDate[];
  dateRanges?: DateRangeEntry[];
}

export interface Restriction {
  id?: number;
  volunteer_id: number;
  schedule_id: number;
  description: string;
  restriction_type_id: number;
  restriction_type?: RestrictionType;
  rules_json?: string | null;
  active?: boolean;
  fixed?: boolean;
  created_at?: string;
  updated_at?: string;
  volunteer?: Volunteer;
  schedule?: Schedule;
}

export interface PaginationInfo {
  page: number;
  page_size: number;
  total_items: number;
  total_pages: number;
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: PaginationInfo;
}
