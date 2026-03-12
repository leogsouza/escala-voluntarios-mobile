export type Weekday = 'SUN' | 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT';

export interface Schedule {
  id: number;
  name: string;
  start_date: string;
  end_date: string;
  status: string;
  duration_months: number;
  church_id: number;
}

export interface Event {
  id: number;
  date: string;
  weekday: Weekday;
  service: string;
  service_code: string;
  time: string;
  template_id: number | null;
  schedule_id: number | null;
  is_special: boolean;
  notes: string;
}

export interface Role {
  id: number;
  name: string;
}

export interface Position {
  id: number;
  name: string;
  role_id: number;
  role?: Role;
}

export interface Volunteer {
  id: number;
  name: string;
  full_name: string;
  active: boolean | null;
  main_role?: Role;
  secondary_role?: Role;
}

export interface Assignment {
  id: number;
  event_id: number;
  volunteer_id: number;
  position_id: number;
  status: string;
  event?: Event;
  position?: Position;
  volunteer?: Volunteer;
}
