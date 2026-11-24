import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { discrepancyListTable, fetchAllDiscrepancies } from './discrepancyListTable';

const translate = (key: string) => key;

describe('discrepancyListTable', () => {
  it('süzgece uyan bütün uyuşmazlıkları okur; fark kilosu sayısal hücredir', async () => {
    server.use(...createHandlers('MANAGER'));
    const all = await fetchAllDiscrepancies({});
    const open = await fetchAllDiscrepancies({ status: 'OPEN' });
    expect(all.length).toBeGreaterThan(open.length);
    expect(open.every((item) => item.status === 'OPEN')).toBe(true);

    const table = discrepancyListTable(all, translate);
    expect(table.rows).toHaveLength(all.length);
    expect(table.headers).toHaveLength(8);
    const [first] = all;
    expect(table.rows[0]?.[0]).toEqual({ kind: 'text', value: first?.discrepancyNo });
  });

  it('boş alanları boş metin olarak yazar', () => {
    const table = discrepancyListTable(
      [
        {
          id: 'disc-1',
          discrepancyNo: 'UY-2026-0001',
          siteId: 'site-1',
          shipmentId: 'shp-1',
          type: 'QUALITY',
          severity: 'RED',
          status: 'OPEN',
          ruleCode: 'QUALITY_CLAIM',
          diffKg: '12.500',
          createdAt: '2026-10-20T08:15:30Z',
        },
      ],
      translate,
    );
    expect(table.rows[0]?.map((cell) => cell.value)).toEqual([
      'UY-2026-0001',
      'discrepancy.type.QUALITY',
      'discrepancy.severity.RED',
      'discrepancy.status.OPEN',
      12.5,
      '',
      '',
      '20.10.2026 11:15',
    ]);
  });
});
