import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const PATH = '/panel/kural-ayarlari';

async function replace(label: RegExp, value: string) {
  const input = await screen.findByLabelText(label);
  await userEvent.clear(input);
  await userEvent.type(input, value);
}

describe('Kural ayarları', () => {
  it('güncel ayarları gösterir ve değişen kuralı geçmişe yeni sürüm olarak ekler', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp(PATH);
    expect(await screen.findByLabelText(/^Yeşil üst sınır/)).toHaveValue('0,50');
    expect(screen.getByLabelText(/^Yolda en çok/)).toHaveValue('24');

    await replace(/^Sarı üst sınır/, '2,25');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));

    expect(await screen.findByText('Ayarlar kaydedildi.')).toBeInTheDocument();
    const tolerance = screen.getByRole('table', { name: 'Tolerans geçmişi' });
    expect(within(tolerance).getAllByRole('row')).toHaveLength(3);
    expect(within(tolerance).getByText('%0,50 / %2,25')).toBeInTheDocument();
    const limits = screen.getByRole('table', { name: 'Yolda süre sınırı geçmişi' });
    expect(within(limits).getAllByRole('row')).toHaveLength(2);
  });

  it('sıralaması bozuk bantları ve sınır dışı saati kaydetmeden gösterir', async () => {
    server.use(...createHandlers('ADMIN'));
    renderApp(PATH);
    await replace(/^Sarı üst sınır/, '0,40');
    await replace(/^Yolda en çok/, '0');
    await userEvent.click(screen.getByRole('button', { name: 'Kaydet' }));

    expect(
      await screen.findByText('Sarı üst sınır Yeşil üst sınırdan büyük olmalı.'),
    ).toBeInTheDocument();
    expect(screen.getByText('1 ile 720 arasında tam saat girin.')).toBeInTheDocument();
    expect(screen.queryByText('Ayarlar kaydedildi.')).not.toBeInTheDocument();
  });

  it('ayar yetkisi olmayan kullanıcıya menüde görünmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp('/panel/uyusmazliklar');
    expect(await screen.findByRole('link', { name: 'Uyuşmazlıklar' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Kural ayarları' })).not.toBeInTheDocument();
  });
});
