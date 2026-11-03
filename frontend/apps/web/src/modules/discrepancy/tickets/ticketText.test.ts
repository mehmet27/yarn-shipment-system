import type { DiscrepancyTicketView } from '@iplik/api-client';
import { describe, expect, it } from 'vitest';
import { enteredNet, netDifference } from './ticketText';

const base: DiscrepancyTicketView = {
  evidenceId: 'e-1',
  side: 'DISPATCH_SCALE',
  status: 'READ',
  problems: [],
  readAt: '2026-10-20T08:05:00Z',
  allowedActions: [],
};

describe('ticketText', () => {
  it('okunan net ile girilen net farkını işaretli kg olarak yazar', () => {
    expect(netDifference({ ...base, netKg: '2000.000', enteredNetKg: '1990.000' })).toBe('+10 kg');
    expect(netDifference({ ...base, netKg: '1990.000', enteredNetKg: '2000.500' })).toBe(
      '−10,5 kg',
    );
    expect(netDifference({ ...base, netKg: '2000.000', enteredNetKg: '2000.000' })).toBe('0 kg');
  });

  it('değerlerden biri yoksa fark ve girilen net boş kalır', () => {
    expect(netDifference({ ...base, netKg: '2000.000' })).toBeUndefined();
    expect(enteredNet(base)).toBeUndefined();
    expect(enteredNet({ ...base, enteredNetKg: '1960.000' })).toBe('1.960 kg');
  });
});
