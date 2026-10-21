import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

async function fill(values: Record<string, string>) {
  for (const [label, value] of Object.entries(values)) {
    await userEvent.type(screen.getByLabelText(label), value);
  }
}

describe('DispatchWeighingPage', () => {
  it('depo çıkış tartısını ve mührü kaydeder, sevkiyat Yüklendi olur', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001');
    await userEvent.click(
      await screen
        .findByRole('link', { name: 'Çıkış tartısını gir' })
        .catch(() => screen.findByRole('button', { name: 'Çıkış tartısını gir' })),
    );
    await fill({
      'Brüt (kg)': '2.000',
      'Dara (kg)': '17,5',
      'Çuval sayısı': '40',
      'Mühür numarası': 'M-NEW',
    });
    expect(screen.getByText('Net: 1.982,5 kg')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText('Yüklendi')).toBeInTheDocument();
  });

  it('hatalı girişte kaydetmeden uyarır', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await userEvent.click(await screen.findByRole('button', { name: 'Kaydet' }));
    expect(await screen.findByText('Kiloyu 1.982,5 biçiminde girin.')).toBeInTheDocument();
  });

  it('kullanılmış mühürde 409 mesajı gösterir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    await screen.findByRole('button', { name: 'Kaydet' });
    await fill({
      'Brüt (kg)': '2.000',
      'Dara (kg)': '17',
      'Çuval sayısı': '40',
      'Mühür numarası': 'M-0002',
    });
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect(
      await screen.findByText('Bu mühür numarası başka bir sevkiyatta kullanılmış.'),
    ).toBeInTheDocument();
  });

  it('çıkış yetkisi olmayan rol 403 sayfasına düşer', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/sevkiyat/shp-0001/cikis');
    expect(await screen.findByText('Bu sayfayı görme yetkiniz yok.')).toBeInTheDocument();
  });
});
