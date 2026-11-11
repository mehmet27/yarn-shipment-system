import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('Merkez iplik stoku', () => {
  it('depo iplik girişi yapar, stok güncellenir; kumaş formunu görmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    expect(await screen.findByText('Henüz iplik girişi yok.')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Merkez kumaş girişi' })).not.toBeInTheDocument();

    const form = screen.getByRole('region', { name: 'Merkeze iplik girişi' });
    await user.type(within(form).getByLabelText(/İplik cinsi/), '30/1 Penye');
    await user.type(within(form).getByLabelText(/Gelen net kg/), '20000');
    await user.click(within(form).getByRole('button', { name: 'İplik girişini kaydet' }));

    const table = await screen.findByRole('table', { name: 'Merkez iplik stoku' });
    expect(within(table).getByText('30/1 Penye')).toBeInTheDocument();
  });

  it('kalite kumaş girer; iplik fire ile stoktan düşer', async () => {
    server.use(...createHandlers('ADMIN'));
    const user = userEvent.setup();
    renderApp('/panel/merkez-stok');
    const receipt = await screen.findByRole('region', { name: 'Merkeze iplik girişi' });
    await user.type(within(receipt).getByLabelText(/İplik cinsi/), 'Penye');
    await user.type(within(receipt).getByLabelText(/Gelen net kg/), '1000');
    await user.click(within(receipt).getByRole('button', { name: 'İplik girişini kaydet' }));
    await screen.findByRole('table', { name: 'Merkez iplik stoku' });

    const form = screen.getByRole('region', { name: 'Merkez kumaş girişi' });
    await user.type(within(form).getByLabelText(/Kumaş cinsi/), 'Süprem');
    await user.type(within(form).getByLabelText('İplik cinsi 1'), 'Penye');
    await user.type(within(form).getByLabelText('Kumaş kg 1'), '480');
    await user.click(within(form).getByRole('button', { name: 'Kumaşı kaydet' }));

    expect(await screen.findByText('Merkez kumaş üretimi')).toBeInTheDocument();
    const table = await screen.findByRole('table', { name: 'Son hareketler' });
    expect(within(table).getByText(/−500/)).toBeInTheDocument();
  });

  it('müdür stoğu görür ama giriş formu yoktur; genel müdür menüde görmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/merkez-stok');
    expect(await screen.findByRole('heading', { name: 'Merkez iplik stoku' })).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Merkeze iplik girişi' })).not.toBeInTheDocument();
  });
});
