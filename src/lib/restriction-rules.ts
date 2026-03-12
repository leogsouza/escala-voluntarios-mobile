const WEEKDAY_NAMES = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

const PERIOD_NAMES: Record<string, string> = {
  morning: 'Manhã',
  afternoon: 'Tarde',
  evening: 'Noite',
  night: 'Noite',
};

const DAY_PATTERNS: Record<string, string> = {
  even: 'Dias pares',
  odd: 'Dias ímpares',
  lastWeek: 'Última semana',
  firstWeek: 'Primeira semana',
  last_week: 'Última semana',
  first_week: 'Primeira semana',
};

function formatDateShort(dateStr: string): string {
  const datePart = dateStr.slice(0, 10);
  const [year, month, day] = datePart.split('-');
  return `${day}/${month}/${year}`;
}

function formatDateCompact(dateStr: string): string {
  const datePart = dateStr.slice(0, 10);
  const [, month, day] = datePart.split('-');
  return `${day}/${month}`;
}

function extractDate(entry: string | { date: string }): string {
  return typeof entry === 'string' ? entry : entry.date;
}

interface RulesSummary {
  summary: string;
  details: string[];
}

export function parseRulesSummary(rulesJson: string | null | undefined): RulesSummary | null {
  if (!rulesJson) return null;

  try {
    const rules = JSON.parse(rulesJson);
    const summaryParts: string[] = [];
    const details: string[] = [];

    if (rules.specificDates?.length > 0) {
      const count = rules.specificDates.length;
      summaryParts.push(`${count} data(s)`);
      const dates = rules.specificDates
        .map((e: string | { date: string }) => formatDateShort(extractDate(e)))
        .join(', ');
      details.push(`Datas: ${dates}`);
    }

    if (rules.dateRanges?.length > 0) {
      const rangeStrs = rules.dateRanges.map(
        (r: { start: string; end: string }) => `${formatDateCompact(r.start)} a ${formatDateCompact(r.end)}`,
      );
      summaryParts.push(rangeStrs.length === 1 ? rangeStrs[0] : `${rangeStrs.length} período(s)`);
      details.push(`Períodos: ${rangeStrs.join(', ')}`);
    }

    if (rules.serviceCodes?.length > 0) {
      summaryParts.push(rules.serviceCodes.join(', '));
      details.push(`Códigos de serviço: ${rules.serviceCodes.join(', ')}`);
    }

    if (rules.weekdays?.length > 0) {
      const days = rules.weekdays.map((d: number) => WEEKDAY_NAMES[d]).join(', ');
      summaryParts.push(days);
      details.push(`Dias da semana: ${days}`);
    }

    if (rules.periods?.length > 0) {
      const periods = rules.periods
        .map((p: string) => PERIOD_NAMES[p.toLowerCase()] || p)
        .join(', ');
      summaryParts.push(periods);
      details.push(`Período do dia: ${periods}`);
    }

    if (rules.dayPattern && rules.dayPattern !== 'all') {
      const label = DAY_PATTERNS[rules.dayPattern] || rules.dayPattern;
      summaryParts.push(label);
      details.push(`Padrão: ${label}`);
    }

    if (rules.limits?.length > 0) {
      summaryParts.push(`${rules.limits.length} limite(s)`);
      details.push(`Limites de frequência: ${rules.limits.length}`);
    }

    if (summaryParts.length === 0) return null;

    const modeLabel = rules.mode === 'include' ? 'Apenas' : 'Exceto';
    return {
      summary: `${modeLabel}: ${summaryParts.join(' · ')}`,
      details,
    };
  } catch {
    return null;
  }
}
