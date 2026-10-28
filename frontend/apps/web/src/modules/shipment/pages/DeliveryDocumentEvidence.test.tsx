import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';
import { captureTicket } from '@/test/ticket';

const RECEIPT = '/api/v1/shipments/:shipmentId/delivery-receipt';
const next = () => userEvent.click(screen.getByRole('button', { name: 'İleri' }));

async function reachDocumentStep() {
  renderApp('/saha/sevkiyat/shp-0004');
  await userEvent.click(await screen.findByRole('link', { name: /Teslim tutanağı/ }));
  await userEvent.click(await screen.findByText('Sağlam'));
  await userEvent.type(screen.getByLabelText('Mühür numarası'), 'M-0004');
  await next();
  await userEvent.type(screen.getByLabelText('Çuval barkodu'), 'B-1{Enter}');
  await next();
  await userEvent.type(screen.getByLabelText('Brüt (kg)'), '1.995,25');
  await userEvent.type(screen.getByLabelText('Dara (kg)'), '17');
  await userEvent.type(screen.getByLabelText('Tartılan çuval sayısı'), '1');
  await captureTicket();
  await screen.findByText('Fiş okundu');
  await next();
}

async function uploadDocument() {
  const file = new File([new Uint8Array(2000)], 'irsaliye.jpg', { type: 'image/jpeg' });
  await userEvent.upload(await screen.findByTestId('upload-camera-input'), file);
  await screen.findByText('İrsaliye yüklendi; tutanağa eklenecek.');
}

describe('teslimde imzalı irsaliye kanıtı', () => {
  it('yüklenen kanıtın kimliğini tutanakta document.signedDocumentEvidenceId olarak gönderir', async () => {
    let body: unknown;
    server.use(...createHandlers('SUBCONTRACTOR'));
    server.use(
      http.post(RECEIPT, async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    await reachDocumentStep();
    await uploadDocument();
    await next();
    await userEvent.click(screen.getByRole('button', { name: 'Tutanağı gönder' }));
    await screen.findByText('Kontrolde').catch(() => undefined);
    expect(JSON.stringify(body)).toMatch(/"document":\{"signedDocumentEvidenceId":"[^"]+"\}/);
  });

  it('422 DELIVERY_EVIDENCE_INVALID için anlamlı mesaj gösterir', async () => {
    server.use(...createHandlers('SUBCONTRACTOR'));
    server.use(
      http.post(RECEIPT, () =>
        HttpResponse.json(
          { status: 422, code: 'DELIVERY_EVIDENCE_INVALID', title: 'x' },
          { status: 422 },
        ),
      ),
    );
    await reachDocumentStep();
    await uploadDocument();
    await next();
    await userEvent.click(screen.getByRole('button', { name: 'Tutanağı gönder' }));
    expect(await screen.findByText(/doğrulanamadı/)).toBeInTheDocument();
  });

  it('502 için irsaliye doğrulanamıyor mesajı gösterir', async () => {
    server.use(...createHandlers('SUBCONTRACTOR'));
    server.use(
      http.post(RECEIPT, () =>
        HttpResponse.json(
          { status: 502, code: 'EVIDENCE_SERVICE_UNAVAILABLE', title: 'x' },
          { status: 502 },
        ),
      ),
    );
    await reachDocumentStep();
    await next();
    await userEvent.click(screen.getByRole('button', { name: 'Tutanağı gönder' }));
    expect(await screen.findByText(/şu an doğrulanamıyor/)).toBeInTheDocument();
  });
});
