import type { DiscrepancySettingsView, UpdateDiscrepancySettingsRequest } from '@iplik/api-client';

/** Backend migration'ındaki ilk kurallar: Yeşil ≤ %0,50, Sarı ≤ %1,50, yolda en çok 24 saat. */
const SYSTEM_ACTOR_ID = '00000000-0000-0000-0000-000000000000';
const SEEDED_AT = '2026-01-01T00:00:00Z';

function initial(): DiscrepancySettingsView {
  return {
    tolerance: { greenMaxPct: '0.50', yellowMaxPct: '1.50' },
    inTransitMaxHours: 24,
    toleranceHistory: [
      { greenMaxPct: '0.50', yellowMaxPct: '1.50', validFrom: SEEDED_AT, setBy: SYSTEM_ACTOR_ID },
    ],
    timeLimitHistory: [{ inTransitMaxHours: 24, validFrom: SEEDED_AT, setBy: SYSTEM_ACTOR_ID }],
  };
}

let settings = initial();

export function mockDiscrepancySettings(): DiscrepancySettingsView {
  return settings;
}

const pct = (value: string) => Number(value).toFixed(2);

/** Backend gibi: yalnızca değişen kural yeni sürüm olur, eskisinin geçerliliği kapanır. */
export function updateMockDiscrepancySettings(
  request: UpdateDiscrepancySettingsRequest,
  userId: string,
): DiscrepancySettingsView {
  const now = new Date().toISOString();
  const tolerance = {
    greenMaxPct: pct(request.greenMaxPct),
    yellowMaxPct: pct(request.yellowMaxPct),
  };
  const isToleranceChanged =
    tolerance.greenMaxPct !== settings.tolerance.greenMaxPct ||
    tolerance.yellowMaxPct !== settings.tolerance.yellowMaxPct;
  const isLimitChanged = request.inTransitMaxHours !== settings.inTransitMaxHours;
  settings = {
    tolerance,
    inTransitMaxHours: request.inTransitMaxHours,
    toleranceHistory: isToleranceChanged
      ? [
          { ...tolerance, validFrom: now, setBy: userId },
          ...settings.toleranceHistory.map((v, i) => (i === 0 ? { ...v, validTo: now } : v)),
        ]
      : settings.toleranceHistory,
    timeLimitHistory: isLimitChanged
      ? [
          { inTransitMaxHours: request.inTransitMaxHours, validFrom: now, setBy: userId },
          ...settings.timeLimitHistory.map((v, i) => (i === 0 ? { ...v, validTo: now } : v)),
        ]
      : settings.timeLimitHistory,
  };
  return settings;
}

export function resetDiscrepancySettings(): void {
  settings = initial();
}
