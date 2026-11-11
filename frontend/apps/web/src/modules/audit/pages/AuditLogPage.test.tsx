import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

describe('Denetim kaydı ekranı', () => {
  it('kayıtları ve zincir doğrulama sonucunu gösterir', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/denetim');
    expect(await screen.findByText(/Zincir doğrulandı/)).toBeInTheDocument();
    expect(await screen.findAllByText('shipment.dispatched')).not.toHaveLength(0);
  });

  it('kayıt türü filtresini adrese yazar', async () => {
    server.use(...createHandlers('ADMIN'));
    const { router } = renderApp('/panel/denetim');
    await userEvent.click(await screen.findByRole('combobox', { name: 'Kayıt türü' }));
    await userEvent.click(await screen.findByRole('option', { name: 'Kanıt' }));
    expect(router.state.location.search).toBe('?varlik=Evidence');
  });

  it('yapan kişiyi UUID yerine ad soyadıyla yazar', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/denetim');
    expect(await screen.findAllByText('Ayşe Demir')).not.toHaveLength(0);
    expect(screen.queryByText(/00000000-0000-7000/)).not.toBeInTheDocument();
  });

  it('izni olmayan rol sayfayı göremez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/denetim');
    expect(await screen.findByText('Bu sayfayı görme yetkiniz yok.')).toBeInTheDocument();
  });
});
