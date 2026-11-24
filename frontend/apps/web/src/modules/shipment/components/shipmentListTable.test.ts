import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { fetchAllShipments, shipmentListTable } from './shipmentListTable';

const translate = (key: string) => key;

describe('shipmentListTable', () => {
  it('süzgece uyan bütün sayfaları okur ve süzgeci sunucuya iletir', async () => {
    const seen: string[] = [];
    server.use(
      http.get('/api/v1/shipments', ({ request }) => {
        seen.push(new URL(request.url).search);
        return undefined;
      }),
      ...createHandlers('MANAGER'),
    );
    const all = await fetchAllShipments({});
    expect(all).toHaveLength(45);
    const bursa = await fetchAllShipments({ search: 'bursa' });
    expect(bursa).toHaveLength(15);
    expect(seen.at(-1)).toContain('search=bursa');
  });

  it('satırları ekrandaki sütunlarla ve Türkiye saatine göre tarihle kurar', () => {
    const table = shipmentListTable(
      [
        {
          shipmentId: 'shp-1',
          shipmentNo: 'SVK-2026-0001',
          siteId: 'site-1',
          status: 'DRAFT',
          subcontractorId: 'sub-1',
          subcontractorTitle: 'Aydın Örme',
          plateNo: '34 ABC 0001',
          shipmentDate: '2026-10-20',
          createdAt: '2026-10-20T08:15:30Z',
          allowedActions: [],
        },
      ],
      translate,
    );
    expect(table.headers).toHaveLength(8);
    expect(table.rows[0]?.map((cell) => cell.value)).toEqual([
      'SVK-2026-0001',
      'shipment.status.DRAFT',
      'shipment.nextStep.DRAFT',
      'Aydın Örme',
      '',
      '34 ABC 0001',
      '20.10.2026',
      '20.10.2026 11:15',
    ]);
  });
});
