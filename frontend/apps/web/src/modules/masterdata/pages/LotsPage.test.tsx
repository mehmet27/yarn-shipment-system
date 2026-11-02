import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

async function choose(label: RegExp, option: string) {
  await userEvent.click(await screen.findByRole('combobox', { name: label }));
  await userEvent.click(await screen.findByRole('option', { name: option }));
}

describe('LotsPage', () => {
  it('kalite kullanıcısı parti açar; yeni parti kilitsiz listelenir', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel/partiler');
    await userEvent.click(await screen.findByRole('button', { name: 'Ekle' }));
    await choose(/Lokasyon/, 'Merkez Depo');
    await userEvent.type(screen.getByLabelText(/Parti numarası/), 'p-2026-0009');
    await choose(/İplik/, 'PES-30-1 · Polyester 30/1');
    await choose(/Kalite sınıfı/, 'A · Birinci kalite');
    await userEvent.type(screen.getByLabelText(/Üretim tarihi/), '2026-10-20');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    const card = await screen.findByRole('group', { name: 'P-2026-0009' });
    expect(within(card).getByText('Kalite kilitli değil')).toBeInTheDocument();
  });

  it('eksik alanları göndermeden gösterir', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp('/panel/partiler');
    await userEvent.click(await screen.findByRole('button', { name: 'Ekle' }));
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));
    expect((await screen.findAllByText('Bu alan boş olamaz.')).length).toBe(5);
  });

  it('kalite onayla kilitlenir; kilitli partide düzenle ve kilitle kalkar', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/partiler');
    const card = await screen.findByRole('group', { name: 'P-2026-0002' });
    await userEvent.click(within(card).getByRole('button', { name: 'Kaliteyi kilitle' }));
    expect(within(card).getByText(/geri alınamaz/)).toBeInTheDocument();
    await userEvent.click(within(card).getByRole('button', { name: 'Kilitle' }));
    expect(await within(card).findByText(/Kalite kilitli ·/)).toBeInTheDocument();
    expect(within(card).queryByRole('button', { name: 'Düzenle' })).not.toBeInTheDocument();
    expect(
      within(card).queryByRole('button', { name: 'Kaliteyi kilitle' }),
    ).not.toBeInTheDocument();
  });

  it('kilitli partide yalnızca pasife alma vardır', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp('/panel/partiler');
    const card = await screen.findByRole('group', { name: 'P-2026-0001' });
    expect(within(card).getByRole('button', { name: 'Pasife al' })).toBeInTheDocument();
    expect(within(card).queryByRole('button', { name: 'Düzenle' })).not.toBeInTheDocument();
  });

  it('LOT_QUALITY_LOCK yetkisi olmayan rol bu sayfaya giremez', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/partiler');
    expect(await screen.findByText(/yetki/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ekle' })).not.toBeInTheDocument();
  });
});
