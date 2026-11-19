import type { FireInvoiceRow } from '@iplik/api-client/settlements';
import { describe, expect, it } from 'vitest';
import { invoicedTable } from './invoicedTable';

const t = (key: string) => key;
const row: FireInvoiceRow = {
  invoiceNo: 'F-1',
  invoicedOn: '2026-11-12',
  shipmentId: 's-1',
  shipmentNo: 'SVK-1',
  subcontractorId: 'sub-1',
  subcontractorTitle: 'Aydın Örme',
  kg: '1500.000',
};

describe('invoicedTable', () => {
  it('sütunlar ekrandakiyle aynı sırada; kilo sayı, tarih Türkçe, sonda toplam', () => {
    const table = invoicedTable([row], '1500.000', t);
    expect(table.headers).toEqual([
      'settlement.invoiced.invoiceNo',
      'settlement.invoiced.date',
      'settlement.invoiced.subcontractor',
      'settlement.invoiced.shipment',
      'settlement.invoiced.kg',
    ]);
    expect(table.rows[0]).toEqual([
      { kind: 'text', value: 'F-1' },
      { kind: 'text', value: '12.11.2026' },
      { kind: 'text', value: 'Aydın Örme' },
      { kind: 'text', value: 'SVK-1' },
      { kind: 'kg', value: 1500 },
    ]);
    expect(table.total?.at(-1)).toEqual({ kind: 'kg', value: 1500 });
  });

  it('kayıt yoksa toplam satırı yazılmaz', () => {
    expect(invoicedTable([], '0', t).total).toBeUndefined();
  });
});
