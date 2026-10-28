import type { DiscrepancyView } from '@iplik/api-client';
import { describe, expect, it } from 'vitest';
import { formatFactValue, ruleLabel, summaryOf } from './discrepancyText';

const t = (key: string) => `[${key}]`;

describe('discrepancyText', () => {
  it('bilinen kural kodunu etikete çevirir, bilinmeyeni olduğu gibi bırakır', () => {
    expect(ruleLabel(t, 'BAG_COUNT_MISMATCH')).toBe('[discrepancy.rule.BAG_COUNT_MISMATCH]');
    expect(ruleLabel(t, 'NEW_RULE')).toBe('NEW_RULE');
  });

  it('kiloyu TR biçiminde, çuval sayısı ve plakayı olduğu gibi yazar', () => {
    expect(formatFactValue(t, 'QUANTITY', '1000.500')).toBe('1.000,5 kg');
    expect(formatFactValue(t, 'QUANTITY', '950')).toBe('950 kg');
    expect(formatFactValue(t, 'BAG', '3')).toBe('3');
    expect(formatFactValue(t, 'DOCUMENT', '34ABC123')).toBe('34ABC123');
    expect(formatFactValue(t, 'BAG', undefined)).toBeUndefined();
  });

  it('fiş tartısını brüt, dara ve net olarak yazar', () => {
    expect(formatFactValue(t, 'DOCUMENT', 'gross=1200.000 tare=250.000 net=950.000')).toBe(
      '[shipment.file.gross] 1.200 kg · [shipment.file.tare] 250 kg · [shipment.file.net] 950 kg',
    );
  });

  it('fark yoksa özet kural ve değerlerden oluşur', () => {
    const bag = {
      type: 'BAG',
      ruleCode: 'BAG_COUNT_MISMATCH',
      expectedValue: '3',
      actualValue: '2',
    } as DiscrepancyView;
    expect(summaryOf(t, bag)).toBe(
      '[discrepancy.rule.BAG_COUNT_MISMATCH] · [discrepancy.facts.expected] 3, [discrepancy.facts.actual] 2',
    );
    expect(
      summaryOf(t, { type: 'DOCUMENT', ruleCode: 'DOCUMENT_MISSING' } as DiscrepancyView),
    ).toBe('[discrepancy.rule.DOCUMENT_MISSING]');
  });
});
