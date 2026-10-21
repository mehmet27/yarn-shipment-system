import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';

describe('ShipmentListPage', () => {
  it('ilk sayfada en yeni 20 sevkiyatı listeler', async () => {
    renderApp('/panel/sevkiyat');
    const table = await screen.findByRole('table');
    expect(within(table).getAllByRole('row')).toHaveLength(21);
    expect(within(table).getByRole('link', { name: 'SVK-2026-0001' })).toHaveAttribute(
      'href',
      '/panel/sevkiyat/shp-0001',
    );
    expect(within(table).getAllByText('1.982,5 kg')).toHaveLength(20);
  });

  it('durum filtresini adrese yazar ve listeyi süzer', async () => {
    const { router } = renderApp('/panel/sevkiyat?sayfa=2');
    await userEvent.click(await screen.findByPlaceholderText('Tüm durumlar'));
    await userEvent.click(await screen.findByRole('option', { name: 'Tamamlandı' }));
    expect(router.state.location.search).toBe('?durum=COMPLETED');
    const table = await screen.findByRole('table');
    expect(await within(table).findAllByText('Tamamlandı')).toHaveLength(5);
  });

  it('sonraki sayfaya geçince sayfa numarasını adrese yazar', async () => {
    const { router } = renderApp('/panel/sevkiyat');
    await userEvent.click(await screen.findByRole('button', { name: '2' }));
    expect(router.state.location.search).toBe('?sayfa=2');
    expect(await screen.findByRole('link', { name: 'SVK-2026-0021' })).toBeInTheDocument();
  });
});
