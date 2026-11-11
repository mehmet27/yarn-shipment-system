import type { DiscrepancyView } from '@iplik/api-client';
import { describe, expect, it } from 'vitest';
import { formatFactValue, ruleLabel, summaryOf } from './discrepancyText';

const t = (key: string) => `[${key}]`;

describe('discrepancyText', () => {
  it('bilinen kural kodunu etikete çevirir, bilinmeyeni olduğu gibi bırakır', () => {
    expect(ruleLabel(t, 'QUALITY_CLAIM')).toBe('[discrepancy.rule.QUALITY_CLAIM]');
    expect(ruleLabel(t, 'NEW_RULE')).toBe('NEW_RULE');
  });

  it('kiloyu TR biçiminde, diğer değerleri olduğu gibi yazar', () => {
    expect(formatFactValue(t, '1000.500')).toBe('1.000,5 kg');
    expect(formatFactValue(t, '34ABC123')).toBe('34ABC123');
    expect(formatFactValue(t, undefined)).toBeUndefined();
  });

  it('fiş tartısını brüt, dara ve net olarak yazar', () => {
    expect(formatFactValue(t, 'gross=1200.000 tare=250.000 net=950.000')).toBe(
      '[shipment.file.gross] 1.200 kg · [shipment.file.tare] 250 kg · [shipment.file.net] 950 kg',
    );
  });

  it('fark yoksa özet kural ve değerlerden oluşur', () => {
    const claim = {
      type: 'QUALITY',
      ruleCode: 'QUALITY_CLAIM',
      expectedValue: '3',
      actualValue: '2',
    } as DiscrepancyView;
    expect(summaryOf(t, claim)).toBe(
      '[discrepancy.rule.QUALITY_CLAIM] · [discrepancy.facts.expected] 3, [discrepancy.facts.actual] 2',
    );
    expect(summaryOf(t, { type: 'QUALITY', ruleCode: 'QUALITY_CLAIM' } as DiscrepancyView)).toBe(
      '[discrepancy.rule.QUALITY_CLAIM]',
    );
  });
});
