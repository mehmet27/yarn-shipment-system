import { getYarnVsFabricReport, type CurrentUser } from '@iplik/api-client';
import type { CentralStock } from '@iplik/api-client/central-stock';
import { CENTRAL_STOCK_PATH, getCentralStock } from '@iplik/api-client/central-stock';
import { getBalances, listFireInvoices, listFireSettlements } from '@iplik/api-client/settlements';
import { formatKgText, sumKg, sumSignedKg } from '@iplik/units';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { addMockReceipt, markMockInvoiced } from '@/mocks/settlements';
import { renderApp } from '@/test/renderApp';
import { sumBySubcontractor } from '../charts/subcontractorYarn';

vi.mock('../export/writePdf', () => ({ downloadPdf: vi.fn(() => Promise.resolve()) }));

type Role = CurrentUser['roles'][number];

const STOCK: CentralStock = {
  rows: [
    { productId: 'yarn-1', yarnType: 'Polyester 30/1', stockKg: '12500.500' },
    { productId: 'yarn-2', yarnType: 'Viskon 30/1', stockKg: '4200.000' },
    { productId: 'yarn-3', yarnType: 'Penye 20/1', stockKg: '-35.250' },
  ],
  totalKg: '16665.250',
  producedFabricKg: '0.000',
  flow: {
    receivedKg: '30000.000',
    directTransferKg: '0.000',
    atSubcontractorsKg: '0.000',
    usedInCentralProductionKg: '0.000',
  },
  movements: [],
  suppliers: [
    { supplierName: 'Şireci', yarnType: 'Polyester 30/1', receivedKg: '20000.000' },
    { supplierName: 'Şireci', yarnType: 'Viskon 30/1', receivedKg: '4200.500' },
    { supplierName: 'Aksa', yarnType: 'Penye 20/1', receivedKg: '5799.500' },
  ],
};

function open(role: Role) {
  server.use(...createHandlers(role));
  server.use(http.get(CENTRAL_STOCK_PATH, () => HttpResponse.json(STOCK)));
  renderApp('/panel/raporlar');
}

/** Çubuk listesinde adı verilen satır. */
function barRow(chart: HTMLElement, name: string): HTMLElement | undefined {
  return within(chart)
    .getAllByRole('listitem')
    .find((item) => within(item).queryByText(name) !== null);
}

/** Bir dosyayı faturalanacak duruma getirir ve iki parça fatura girer (2,5 kg). */
function seedInvoices(): void {
  addMockReceipt(
    'shp-0003',
    { fabricType: 'Süprem', netKg: '1900', waybillNo: 'K-1' },
    'user-quality',
  );
  const base = { userId: 'user-manager' } as const;
  markMockInvoiced('shp-0003', {
    ...base,
    invoiceNo: 'F-10',
    invoicedAt: '2026-11-05',
    lines: [{ yarnType: '30/1 Penye', netKg: '1' }],
  });
  markMockInvoiced('shp-0003', {
    ...base,
    invoiceNo: 'F-11',
    invoicedAt: '2026-11-12',
    lines: [{ yarnType: '30/1 Penye', netKg: '1.5' }],
  });
}

/** Grafiklerdeki rakamlar, aynı verinin geldiği diğer ekranların (Bakiye, Faturalama, Merkez stok) uçlarıyla tutar. */
describe('Raporlar Özet sekmesi grafikleri', () => {
  it('fasoncularda iplik çubukları Fasoncu bakiyesi ucunun fasoncu toplamlarıyla birebir aynıdır', async () => {
    open('GENERAL_MANAGER');
    const chart = await screen.findByTestId('chart-subcontractor-yarn');
    const balances = await getBalances();
    const expected = sumBySubcontractor(balances);
    expect(expected.length).toBeGreaterThan(0);
    for (const row of expected) {
      const item = barRow(chart, row.subcontractorTitle);
      expect(item).toHaveTextContent(formatKgText(row.balanceKg));
    }
    expect(Number(sumSignedKg(expected.map((row) => row.balanceKg)))).toBe(
      Number(balances.totalKg),
    );
    expect(await screen.findByTestId('stat-subcontractor-yarn')).toHaveTextContent(
      formatKgText(balances.totalKg),
    );
  });

  it('faturalama çubukları Faturalama ekranının iki sekmesinin toplamlarıyla aynıdır', async () => {
    seedInvoices();
    open('GENERAL_MANAGER');
    const chart = await screen.findByTestId('chart-invoicing');
    const waiting = await listFireSettlements({ status: 'TO_INVOICE', page: 0, size: 100 });
    const toInvoiceKg = sumKg(waiting.items.map((row) => row.remainingKg));
    const invoiced = await listFireInvoices({ page: 0, size: 1 });
    expect(Number(toInvoiceKg)).toBeGreaterThan(0);
    expect(Number(invoiced.totalKg)).toBe(Number('2.5'));
    await within(chart).findByText(formatKgText(toInvoiceKg));
    const [toInvoiceRow, invoicedRow] = within(chart).getAllByRole('listitem');
    expect(toInvoiceRow).toHaveTextContent('Faturalanmayı bekleyen');
    expect(toInvoiceRow).toHaveTextContent(formatKgText(toInvoiceKg));
    expect(invoicedRow).toHaveTextContent('Faturalanan');
    expect(invoicedRow).toHaveTextContent(formatKgText(invoiced.totalKg));
  });

  it('merkez depodaki iplik çubukları Merkez iplik stoku ucunun satırlarıyla aynıdır', async () => {
    open('GENERAL_MANAGER');
    const chart = await screen.findByTestId('chart-central-stock');
    const stock = await getCentralStock();
    expect(stock.rows.length).toBeGreaterThan(0);
    for (const row of stock.rows) {
      const item = barRow(chart, row.yarnType);
      expect(item).toHaveTextContent(formatKgText(row.stockKg));
    }
    expect(within(chart).getByText(/Eksi: iplik girişi/)).toBeInTheDocument();
    expect(Number(sumSignedKg(stock.rows.map((row) => row.stockKg)))).toBe(Number(stock.totalKg));
  });

  it('aylık eğilim tablosu son ayda "Giden iplik ↔ gelen kumaş" raporunun toplamını gösterir', async () => {
    open('MANAGER');
    const table = await screen.findByTestId('trend-table');
    const rows = await getYarnVsFabricReport();
    const lastMonth = within(table).getByRole('row', { name: /Kas 26/ });
    expect(lastMonth).toHaveTextContent(
      formatKgText(sumKg(rows.map((row) => row.sentKg))).replace(' kg', ''),
    );
    expect(lastMonth).toHaveTextContent('%10,00');
    expect(within(table).getByRole('row', { name: /Haz 26/ })).toHaveTextContent('—');
    expect(within(table).getAllByRole('row')).toHaveLength(7);
  });

  it('fire çubukları sınırı aşan fasoncuyu yazıyla da söyler; aynı çubuklar iplik-kumaş sekmesinde tabloyla durur', async () => {
    open('MANAGER');
    const chart = await screen.findByTestId('chart-fire');
    expect(within(chart).getByText('Kırıkhan Tekstil')).toBeInTheDocument();
    expect(within(chart).getByText('%10,00')).toBeInTheDocument();
    expect(within(chart).getByText(/1 sevkiyat fire sınırını aştı/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('tab', { name: 'Giden iplik ↔ gelen kumaş' }));
    const row = await screen.findByRole('row', { name: /Kırıkhan Tekstil/ });
    expect(row).toHaveTextContent('%10,00');
  });

  it('sevkiyat durum çubuğu sayıları açıklamada yazıyla verir', async () => {
    open('MANAGER');
    const chart = await screen.findByTestId('chart-shipment-status');
    expect(within(chart).getByRole('img')).toHaveAccessibleName(
      'Sevkiyatların durumu: Süren 4, İncelemede 2, Tamamlanan 5, İptal 1',
    );
    expect(within(chart).getAllByTestId('segmented-bar-part')).toHaveLength(4);
  });

  it('tedarikçi çubukları tablodaki tedarikçi toplamlarıyla aynıdır', async () => {
    open('MANAGER');
    const chart = await screen.findByTestId('chart-suppliers');
    const stock = await getCentralStock();
    const first = stock.suppliers[0];
    expect(first).toBeDefined();
    const total = sumKg(
      stock.suppliers
        .filter((row) => row.supplierName === first?.supplierName)
        .map((row) => row.receivedKg),
    );
    const item = barRow(chart, first?.supplierName ?? '');
    expect(item).toHaveTextContent(formatKgText(total));
  });

  it('bakiye yetkisi olmayan rol fasoncu ve faturalama grafiklerini görmez', async () => {
    open('WAREHOUSE');
    await screen.findByRole('heading', { level: 1 });
    expect(screen.queryByTestId('chart-subcontractor-yarn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('chart-invoicing')).not.toBeInTheDocument();
  });
});
