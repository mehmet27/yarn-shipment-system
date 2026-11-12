import { describe, expect, it } from 'vitest';
import { EMPTY_CREATE_DRAFT, newLine } from './createDraft';
import { buildOrderFormDefinition } from './orderFormPdf';
import type { OrderFormTexts } from './orderFormPdf';

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
  notes: ['1 - Not'],
  orderApproval: 'SİPARİŞ ONAYI',
  producer: 'ÜRETİCİ FİRMA',
  approver: 'FATİH AYTÜRK',
  widthTypes: { OPEN_WIDTH: 'AÇIK EN' },
};

describe('buildOrderFormDefinition', () => {
  it('satırları, boş satırları ve toplamı basar', () => {
    const line = {
      ...newLine(),
      fabricType: '30/150 PENYE',
      widthType: 'OPEN_WIDTH',
      netKg: '3850',
      unitPrice: '21',
    };
    const definition = buildOrderFormDefinition(
      { ...EMPTY_CREATE_DRAFT, lines: [line, { ...newLine(), netKg: '12.100' }] },
      TEXTS,
      '09/11/2026',
    );
    const text = JSON.stringify(definition.content);
    expect(text).toContain('30/150 PENYE');
    expect(text).toContain('AÇIK EN');
    expect(text).toContain('21,00 TL');
    expect(text).toContain('09/11/2026');
  });
});
