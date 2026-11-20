import type { YarnVsFabricRowView } from '@iplik/api-client';
import { describe, expect, it } from 'vitest';
import { toFireBars } from './fireBars';

const t = (key: string, options?: Record<string, unknown>) =>
  `${key} ${JSON.stringify(options ?? {})}`;

const row = (name: string, rate: string, overLimit: number): YarnVsFabricRowView => ({
  subcontractorId: name,
  subcontractorName: name,
  sentKg: '1000.000',
  returnedYarnKg: '0.000',
  fabricKg: '900.000',
  actualFireRate: rate,
  shortageKg: '60.000',
  overLimitShipmentCount: overLimit,
  overLimitFireKg: '100.000',
});

describe('toFireBars', () => {
  it('fire oranı en yüksek fasoncuyu üste alır ve oranı yüzde olarak yazar', () => {
    const bars = toFireBars([row('Aksu', '0.0400', 0), row('Kırıkhan', '0.1000', 1)], t);
    expect(bars.map((bar) => bar.label)).toEqual(['Kırıkhan', 'Aksu']);
    expect(bars[0]?.valueLabel).toBe('%10,00');
    expect(bars[1]?.valueLabel).toBe('%4,00');
  });

  it('fire sınırını aşan fasoncuyu renkle birlikte yazıyla da işaretler', () => {
    const [over, within] = toFireBars([row('Kırıkhan', '0.1000', 2), row('Aksu', '0.0400', 0)], t);
    expect(over?.tone).toBe('danger');
    expect(over?.note).toContain('report.charts.fire.overLimit');
    expect(over?.note).toContain('"count":2');
    expect(within?.tone).toBe('neutral');
    expect(within?.note).not.toContain('overLimit');
  });
});
