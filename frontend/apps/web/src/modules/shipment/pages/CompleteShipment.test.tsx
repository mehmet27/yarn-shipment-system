import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const AWAITING_FILE = '/panel/sevkiyat/shp-0002';
const waybill = () => new File(['irsaliye'], 'irsaliye.pdf', { type: 'application/pdf' });

describe('İrsaliyeyle tamamlama', () => {
  it('müdür irsaliyeyi yükleyip numarasını yazınca sevkiyat tamamlanır', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(AWAITING_FILE);
    const confirm = await screen.findByRole('button', { name: 'Sevkiyatı tamamla' });
    expect(confirm).toBeDisabled();
    const [fileInput] = screen.getAllByTestId('upload-file-input');
    if (fileInput === undefined) throw new Error('dosya girişi yok');
    await userEvent.upload(fileInput, waybill());
    expect(await screen.findByText('İrsaliye yüklendi')).toBeInTheDocument();
    expect(confirm).toBeDisabled();
    await userEvent.type(screen.getByLabelText(/İrsaliye no/), 'IRS-2026-77');
    await userEvent.click(confirm);
    expect(await screen.findByTestId('shipment-status')).toHaveTextContent('Tamamlandı');
    expect(screen.queryByRole('button', { name: 'Sevkiyatı tamamla' })).not.toBeInTheDocument();
  });

  it('irsaliye yüklenmeden tamamlama düğmesi kapalı kalır', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(AWAITING_FILE);
    await userEvent.type(await screen.findByLabelText(/İrsaliye no/), 'IRS-1');
    expect(screen.getByRole('button', { name: 'Sevkiyatı tamamla' })).toBeDisabled();
  });

  it('depo tamamlama formunu görmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp(AWAITING_FILE);
    await screen.findByRole('heading', { name: 'SVK-2026-0002' });
    expect(screen.queryByRole('button', { name: 'Sevkiyatı tamamla' })).not.toBeInTheDocument();
  });
});
