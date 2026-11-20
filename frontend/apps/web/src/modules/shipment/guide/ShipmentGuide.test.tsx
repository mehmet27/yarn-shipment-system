import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { addBagsTotalling1982Point5 } from '@/test/bags';
import { renderApp } from '@/test/renderApp';

const DRAFT = '/panel/sevkiyat/shp-0001';
const IN_TRANSIT = '/panel/sevkiyat/shp-0002';
const COMPLETED = '/panel/sevkiyat/shp-0007';

describe('Sevkiyat dosyası: sıradaki adım', () => {
  it('altı adımı ve her adımı kimin yaptığını gösterir; geçerli adım işaretlidir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp(DRAFT);
    const steps = within(await screen.findByRole('list', { name: 'Sevkiyat süreci' }));
    const items = steps.getAllByRole('listitem');
    expect(items.map((item) => item.textContent)).toEqual([
      'Yeni sevkiyatMüdür',
      '2ÇuvallarDepo',
      '3Çıkış tartısıDepo',
      '4İrsaliyeMüdür',
      '5Kumaş girişiKalite',
      '6Fire dosyasıMüdür',
    ]);
    expect(items[1]).toHaveAttribute('aria-current', 'step');
  });

  it('depo çuvalsız taslakta "Sıra sizde" ve tek düğme olarak "Çuval eklemeye başla" görür; düğme sekmeyi açar', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp(DRAFT);
    const card = within(await screen.findByTestId('next-step'));
    expect(card.getByText('Sıra sizde')).toBeInTheDocument();
    expect(card.getByText('Çuvalları ekleyin')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Çıkış tartısını gir' })).not.toBeInTheDocument();
    await userEvent.click(card.getByRole('button', { name: 'Çuval eklemeye başla' }));
    expect(await screen.findByRole('tab', { name: 'Çuvallar' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
  });

  it('çuvallar eklenince depoya "Çıkış tartısını gir" düğmesi gelir', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    addBagsTotalling1982Point5();
    renderApp(DRAFT);
    const card = within(await screen.findByTestId('next-step'));
    expect(card.getByText('Çıkış tartısını girin')).toBeInTheDocument();
    expect(card.getByRole('link', { name: 'Çıkış tartısını gir' })).toHaveAttribute(
      'href',
      '/panel/sevkiyat/shp-0001/cikis',
    );
  });

  it('müdür taslakta sıranın depoda olduğunu okur; düğme görmez', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(DRAFT);
    const card = within(await screen.findByTestId('next-step'));
    expect(card.getByText('Çuvallar eklenecek')).toBeInTheDocument();
    expect(card.getByText(/Bu adımı Depo yapacak\./)).toBeInTheDocument();
    expect(card.queryByText('Sıra sizde')).not.toBeInTheDocument();
    expect(screen.queryByTestId('guide-action')).not.toBeInTheDocument();
  });

  it('yoldaki sevkiyatta sıra müdürdedir; depo bekler', async () => {
    server.use(...createHandlers('MANAGER'));
    renderApp(IN_TRANSIT);
    const card = within(await screen.findByTestId('next-step'));
    expect(card.getByText('İrsaliyeyi yükleyip sevkiyatı tamamlayın')).toBeInTheDocument();
    expect(card.getByRole('button', { name: 'İrsaliyeyi yükle' })).toBeInTheDocument();
  });

  it('tamamlanmış sevkiyatta kumaş girilmemişse sıra kalitededir; müdür bekler', async () => {
    server.use(...createHandlers('QUALITY'));
    renderApp(COMPLETED);
    const card = within(await screen.findByTestId('next-step'));
    expect(card.getByText('Gelen kumaşı girin')).toBeInTheDocument();
    await userEvent.click(card.getByRole('button', { name: 'Kumaş girişi yap' }));
    expect(await screen.findByRole('tab', { name: 'Kumaş girişi' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
  });

  it('depo tamamlanmış sevkiyatta fire dosyasını okumaz; teslim edildiğini görür', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp(COMPLETED);
    const card = within(await screen.findByTestId('next-step'));
    expect(card.getByText('Fasoncuya teslim edildi')).toBeInTheDocument();
    expect(screen.queryByTestId('guide-action')).not.toBeInTheDocument();
  });
});
