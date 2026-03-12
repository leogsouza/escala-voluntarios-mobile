export type ServiceType = 'culto' | 'ensaio' | string;

type ServiceTypeColors = {
  background: string;
  text: string;
};

const SERVICE_TYPE_COLORS: Record<string, ServiceTypeColors> = {
  culto: {
    background: '#3B82F6',
    text: '#1D4ED8'
  },
  ensaio: {
    background: '#F97316',
    text: '#C2410C'
  },
  default: {
    background: '#8B5CF6',
    text: '#6D28D9'
  }
};

export const BRAND_COLORS = {
  primary: '#1e3a5f',
  accent: '#7C3AED'
};

export function getServiceTypeColors(type: ServiceType): ServiceTypeColors {
  return SERVICE_TYPE_COLORS[type] ?? SERVICE_TYPE_COLORS.default;
}

export { SERVICE_TYPE_COLORS };
