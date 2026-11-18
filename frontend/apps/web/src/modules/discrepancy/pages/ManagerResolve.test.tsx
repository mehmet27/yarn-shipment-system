import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const SETTLEMENT = {
  shipmentId: 'shipment-1',
  status: 'TO_INVOICE',
  fireRate: '0.05',
  sentKg: '1000',
  returnedKg: '0',
  claimKg: '0',
  fabricKg: '900',
  baseKg: '1000',
  expectedFabricKg: '950',
  shortageKg: '50',
  faultyKg: '50',
  actualFirePercent: '10',
};

/** Uyuşmazlık `disc-N` sevkiyat `shipment-N`'e bağlıdır; fire dosyası faturalanmayı bekliyor. */
const toInvoice = (n = 1) =>
  http.get(`/api/v1/shipments/shipment-${String(n)}/fire-settlement`, () =>
    HttpResponse.json({ ...SETTLEMENT, shipmentId: `shipment-${String(n)}` }),
  );

async function chooseAs(label: string) {
  await userEvent.click(await screen.findByRole('combobox', { name: /^Karar/ }));
  await userEvent.click(await screen.findByRole('option', { name: label }));
}

describe('Müdür kalite itirazını kapatır', () => {
  it('yalnızca haklı ya da haksız der; atama ve sorumluluk kararı çıkmaz', async () => {
    server.use(toInvoice(), ...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar/disc-1');
    await userEvent.click(await screen.findByRole('combobox', { name: /^Karar/ }));
    expect(await screen.findByRole('option', { name: 'Haklı, faturalandı' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Haklı' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Haksız' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Sorumluluk atandı' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Bana ata' })).not.toBeInTheDocument();
  });

  it('fire dosyası fatura beklemiyorsa "Haklı, faturalandı" çıkmaz', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar/disc-1');
    await userEvent.click(await screen.findByRole('combobox', { name: /^Karar/ }));
    expect(await screen.findByRole('option', { name: 'Haklı' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Haklı, faturalandı' })).not.toBeInTheDocument();
  });

  it('haklı kararı ile faturayı tek adımda kaydeder', async () => {
    let invoiceBody: unknown;
    server.use(
      toInvoice(),
      http.post('/api/v1/shipments/shipment-1/fire-settlement/invoice', async ({ request }) => {
        invoiceBody = await request.json();
        return HttpResponse.json({ ...SETTLEMENT, status: 'INVOICED' });
      }),
      ...createHandlers('MANAGER'),
    );
    renderApp('/panel/uyusmazliklar/disc-1');
    await chooseAs('Haklı, faturalandı');
    const save = await screen.findByRole('button', { name: 'Karara bağla' });
    await userEvent.type(screen.getByLabelText(/^Not(?! \()/), 'Kumaş bozuk çıktı');
    expect(save).toBeDisabled();
    await userEvent.type(screen.getByLabelText(/^Fatura no/), 'F-77');
    await userEvent.click(save);
    expect(screen.getByText(/Karar: haklı\. Fatura F-77 kaydedilecek/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Evet, kararı kaydet' }));
    expect(await screen.findAllByText('Kumaş bozuk çıktı')).not.toHaveLength(0);
    expect(invoiceBody).toMatchObject({ invoiceNo: 'F-77' });
  });

  it('fatura yazılamazsa kararın kaydedildiğini söyler ve fire sekmesine yönlendirir', async () => {
    server.use(
      toInvoice(2),
      http.post('/api/v1/shipments/shipment-2/fire-settlement/invoice', () =>
        HttpResponse.json(
          { status: 409, code: 'FIRE_INVOICE_NOT_APPLICABLE', title: 'x' },
          { status: 409 },
        ),
      ),
      ...createHandlers('MANAGER'),
    );
    renderApp('/panel/uyusmazliklar/disc-2');
    await chooseAs('Haklı, faturalandı');
    await userEvent.type(await screen.findByLabelText(/^Not(?! \()/), 'Haklı');
    await userEvent.type(screen.getByLabelText(/^Fatura no/), 'F-1');
    await userEvent.click(screen.getByRole('button', { name: 'Karara bağla' }));
    await userEvent.click(screen.getByRole('button', { name: 'Evet, kararı kaydet' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Karar kaydedildi ama fatura');
    await userEvent.click(screen.getByRole('button', { name: 'Tamam' }));
    expect(await screen.findAllByText('Haklı')).not.toHaveLength(0);
  });

  it('haksız kararı tek başına kapatır', async () => {
    server.use(toInvoice(5), ...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar/disc-5');
    await chooseAs('Haksız');
    await userEvent.type(await screen.findByLabelText(/^Not(?! \()/), 'Itiraz dayanaksız');
    await userEvent.click(screen.getByRole('button', { name: 'Karara bağla' }));
    await userEvent.click(screen.getByRole('button', { name: 'Evet, kararı kaydet' }));
    expect(await screen.findAllByText('Itiraz dayanaksız')).not.toHaveLength(0);
  });
});
