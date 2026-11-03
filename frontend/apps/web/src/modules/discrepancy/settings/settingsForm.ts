import type { DiscrepancySettingsView, UpdateDiscrepancySettingsRequest } from '@iplik/api-client';

export const MIN_HOURS = 1;
export const MAX_HOURS = 720;
const MAX_PCT = 100;
/** Backend Pct şeması: en çok üç basamak tam, iki basamak ondalık. */
const PCT_PATTERN = /^\d{1,3}(\.\d{1,2})?$/;
const HOURS_PATTERN = /^\d{1,3}$/;

export type SettingsDraft = {
  greenMaxPct: string;
  yellowMaxPct: string;
  inTransitMaxHours: string;
};

/** Değerler `discrepancy.settings.errors.*` anahtarlarıdır. */
export type DraftErrors = Partial<Record<keyof SettingsDraft, 'pct' | 'order' | 'hours'>>;

export function draftOf(settings: DiscrepancySettingsView): SettingsDraft {
  return {
    greenMaxPct: settings.tolerance.greenMaxPct.replace('.', ','),
    yellowMaxPct: settings.tolerance.yellowMaxPct.replace('.', ','),
    inTransitMaxHours: String(settings.inTransitMaxHours),
  };
}

/** Türkçe ondalık virgül kabul edilir; API'ye noktalı gider. */
function pctOf(value: string): string | undefined {
  const normalized = value.trim().replace(',', '.');
  return PCT_PATTERN.test(normalized) && Number(normalized) <= MAX_PCT ? normalized : undefined;
}

function hoursOf(value: string): number | undefined {
  const hours = Number(value.trim());
  return HOURS_PATTERN.test(value.trim()) && hours >= MIN_HOURS && hours <= MAX_HOURS
    ? hours
    : undefined;
}

function errorsOf(parsed: {
  green: string | undefined;
  yellow: string | undefined;
  hours: number | undefined;
  isOrdered: boolean;
}): DraftErrors {
  return {
    ...(parsed.green === undefined ? { greenMaxPct: 'pct' } : {}),
    ...(parsed.yellow === undefined ? { yellowMaxPct: 'pct' } : {}),
    ...(parsed.isOrdered ? {} : { yellowMaxPct: 'order' }),
    ...(parsed.hours === undefined ? { inTransitMaxHours: 'hours' } : {}),
  };
}

/** Backend kurallarının aynısı (0 ≤ Yeşil < Sarı ≤ 100, 1–720 saat); hata yoksa istek döner. */
export function validateDraft(
  draft: SettingsDraft,
): { request: UpdateDiscrepancySettingsRequest } | { errors: DraftErrors } {
  const green = pctOf(draft.greenMaxPct);
  const yellow = pctOf(draft.yellowMaxPct);
  const hours = hoursOf(draft.inTransitMaxHours);
  const isOrdered = green === undefined || yellow === undefined || Number(green) < Number(yellow);
  const errors = errorsOf({ green, yellow, hours, isOrdered });
  return green === undefined || yellow === undefined || hours === undefined || !isOrdered
    ? { errors }
    : { request: { greenMaxPct: green, yellowMaxPct: yellow, inTransitMaxHours: hours } };
}
