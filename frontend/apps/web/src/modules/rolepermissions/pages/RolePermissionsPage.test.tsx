import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { resetMockRolePermissions } from '@/mocks/rolePermissionHandlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const PATH = '/panel/yetkiler';
const REPORTS = 'Raporlar ekranını görür';

describe('Yetkiler', () => {
  beforeEach(() => {
    resetMockRolePermissions();
  });

  it('yönetici bir role izin ekleyip kaydeder', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp(PATH);
    await userEvent.click(await screen.findByRole('tab', { name: 'Depo' }));
    const box = await screen.findByRole('checkbox', { name: REPORTS });
    expect(box).not.toBeChecked();

    await userEvent.click(box);
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));

    expect(await screen.findByText('Yetkiler kaydedildi.')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: REPORTS })).toBeChecked();
  });

  it('yönetici bir rolden izin kaldırır ve varsayılana döndürür', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp(PATH);
    await userEvent.click(await screen.findByRole('tab', { name: 'Müdür' }));
    const box = await screen.findByRole('checkbox', {
      name: 'Tanımlar ekranı: kayıt ekler, düzenler, pasife alır',
    });
    expect(box).toBeChecked();

    await userEvent.click(box);
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText('Yetkiler kaydedildi.')).toBeInTheDocument();
    expect(box).not.toBeChecked();

    await userEvent.click(screen.getByRole('button', { name: 'Varsayılana dön' }));
    expect(await screen.findByText('Varsayılan yetkiler geri yüklendi.')).toBeInTheDocument();
    expect(
      await screen.findByRole('checkbox', {
        name: 'Tanımlar ekranı: kayıt ekler, düzenler, pasife alır',
      }),
    ).toBeChecked();
  });

  it('yalnızca yöneticide olan izinler başka role verilemez', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp(PATH);
    await userEvent.click(await screen.findByRole('tab', { name: 'Müdür' }));

    const users = await screen.findByRole('checkbox', { name: 'Kullanıcılar ekranı' });
    expect(users).toBeDisabled();
    expect(users).not.toBeChecked();
    expect(within(screen.getByRole('tabpanel')).getAllByText('Yalnızca yönetici')).toHaveLength(3);
  });

  it('yönetici rolünün yetkileri değiştirilemez', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp(PATH);
    await userEvent.click(await screen.findByRole('tab', { name: 'Yönetici' }));

    expect(
      await screen.findByText('Yönetici rolünün yetkileri değiştirilemez.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Yetkiler ekranı' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Kaydet' })).not.toBeInTheDocument();
  });

  it('müdür Yetkiler ekranına giremez ve menüde görmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(PATH);

    expect(await screen.findByText(/yetki/i)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Yetkiler' })).not.toBeInTheDocument();
  });
});
