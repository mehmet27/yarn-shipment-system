import { describe, expect, it } from 'vitest';
import { buildOrderFormDefinition } from './orderFormPdf';
import type { OrderFormTexts } from './orderFormPdf';
import { shipmentOrderDraft } from './shipmentOrderDraft';

const TEXTS: OrderFormTexts = {
  company: 'GENCER',
  companyLine: 'TEKSTİL',
  address: 'Adres',
  firm: 'FİRMA',
  orderNo: 'SİPARİŞ NO',
  contact: 'YETKİLİ KİŞİ',
  date: 'TARİHİ',
  columns: ['A', 'B', 'C', 'D', 'E', 'F', 'G'],
  total: 'TOPLAM',
  notesTitle: 'AÇIKLAMALAR :',
  notes: [],
  orderApproval: 'SİPARİŞ ONAYI',
  producer: 'ÜRETİCİ FİRMA',
  approver: 'FATİH AYTÜRK',
  widthTypes: { TUBE: 'TÜP', MAY: 'MAY' },
};

describe('shipmentOrderDraft', () => {
  it('kayıtlı reçeteyi PDF taslağına çevirir; kilo ve fiyat doğru biçimde basılır', () => {
    const draft = shipmentOrderDraft('Örnek Fasoncu', [
      {
        lineNo: 1,
        netKg: '1050.000',
        fabricType: 'Penye',
        lycra: '30 Denye',
        widthType: 'TUBE',
        yarnLength: '1247',
        pussFine: 'Puss',
        unitPrice: '1250.0000',
      },
      { lineNo: 2, netKg: '250.000', fabricType: 'Kumaş', unitPrice: '1045.0000' },
    ]);
    const text = JSON.stringify(buildOrderFormDefinition(draft, TEXTS, '16/11/2026').content);
    expect(text).toContain('Örnek Fasoncu');
    expect(text).toContain('Penye');
    expect(text).toContain('TÜP');
    expect(text).toContain('1.050');
    expect(text).toContain('1.250,00 TL');
    expect(text).toContain('1.045,00 TL');
    expect(text).toContain('1.300');
  });
});
