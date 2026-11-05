import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { resetMockBranding } from '@/mocks/brandingHandlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const PATH = '/panel/gorunum';

describe('Görünüm ve marka', () => {
  beforeEach(() => {
    resetMockBranding();
    window.localStorage.clear();
  });

  it('yönetici firma adını kaydeder; üst bar ve önizleme yeni adı gösterir', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp(PATH);
    const name = await screen.findByLabelText(/^Firma adı/);

    await userEvent.type(name, 'Gencer Fabric');
    expect(
      within(screen.getByLabelText('Önizleme')).getByText('Gencer Fabric'),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));

    expect(await screen.findByText('Görünüm kaydedildi.')).toBeInTheDocument();
    expect(screen.getAllByText('Gencer Fabric').length).toBeGreaterThan(1);
  });

  it('geçersiz rengi kaydetmeden uyarır', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp(PATH);
    const color = await screen.findByLabelText(/^Ana renk/);

    await userEvent.clear(color);
    await userEvent.type(color, 'mavi');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));

    expect(await screen.findByText('Rengi #rrggbb biçiminde girin.')).toBeInTheDocument();
    expect(screen.queryByText('Görünüm kaydedildi.')).not.toBeInTheDocument();
  });

  it('yetkisiz rol Görünüm menüsünü görmez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat');

    await screen.findByRole('heading', { name: 'Sevkiyatlar' });
    expect(screen.queryByRole('link', { name: 'Görünüm' })).not.toBeInTheDocument();
  });
});
