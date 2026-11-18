import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

async function chooseAs(label: string) {
  await userEvent.click(await screen.findByRole('combobox', { name: /^Karar/ }));
  await userEvent.click(await screen.findByRole('option', { name: label }));
}

describe('Müdür kalite itirazını kapatır', () => {
  it('yalnızca haklı ya da haksız der; atama ve fatura seçeneği çıkmaz', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar/disc-1');
    await userEvent.click(await screen.findByRole('combobox', { name: /^Karar/ }));
    expect(await screen.findByRole('option', { name: 'Haklı' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Haksız' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Haklı, faturalandı' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Bana ata' })).not.toBeInTheDocument();
  });

  it('haklı kararı onaydan sonra kaydeder; fatura alanı çıkmaz', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar/disc-1');
    await chooseAs('Haklı');
    await userEvent.type(await screen.findByLabelText(/^Not(?! \()/), 'Kumaş bozuk çıktı');
    expect(screen.queryByLabelText(/^Fatura no/)).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Karara bağla' }));
    await userEvent.click(screen.getByRole('button', { name: 'Evet, kararı kaydet' }));
    expect(await screen.findAllByText('Kumaş bozuk çıktı')).not.toHaveLength(0);
  });

  it('haksız kararı tek başına kapatır', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar/disc-5');
    await chooseAs('Haksız');
    await userEvent.type(await screen.findByLabelText(/^Not(?! \()/), 'Itiraz dayanaksız');
    await userEvent.click(screen.getByRole('button', { name: 'Karara bağla' }));
    await userEvent.click(screen.getByRole('button', { name: 'Evet, kararı kaydet' }));
    expect(await screen.findAllByText('Itiraz dayanaksız')).not.toHaveLength(0);
  });
});
