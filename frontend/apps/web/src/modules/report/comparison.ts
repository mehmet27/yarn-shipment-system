const MS_PER_DAY = 86_400_000;
const PERCENT = 100;

export type Period = { from: string; to: string };

const toDay = (iso: string) => Date.parse(`${iso}T00:00:00Z`);
const toIso = (ms: number) => new Date(ms).toISOString().slice(0, 'YYYY-MM-DD'.length);

/**
 * Seçilen dönemin hemen öncesindeki, aynı uzunluktaki dönem: 1-30 Kasım seçiliyse 2-31 Ekim. ISO tarihler takvim günü
 * olarak ele alınır; saat dilimi hesabı yoktur.
 */
export function previousPeriod(period: Period): Period {
  const from = toDay(period.from);
  const length = toDay(period.to) - from + MS_PER_DAY;
  return { from: toIso(from - length), to: toIso(from - MS_PER_DAY) };
}

export type Change = { direction: 'up' | 'down' | 'same'; percent: number };

/** Önceki döneme göre değişim, tam yüzdeye yuvarlanır; önceki dönem sıfırsa oran hesaplanamaz (`null`). */
export function toChange(current: number, previous: number): Change | null {
  if (!Number.isFinite(current) || !Number.isFinite(previous)) return null;
  if (current === previous) return { direction: 'same', percent: 0 };
  if (previous === 0) return null;
  const percent = Math.round((Math.abs(current - previous) / Math.abs(previous)) * PERCENT);
  if (percent === 0) return { direction: 'same', percent: 0 };
  return { direction: current > previous ? 'up' : 'down', percent };
}
