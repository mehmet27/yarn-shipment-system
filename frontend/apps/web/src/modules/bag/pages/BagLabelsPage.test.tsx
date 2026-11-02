import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeAll, describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { renderApp } from '@/test/renderApp';

const PRINTED_KEY = 'BAG-7K3M-Q9XD';

beforeAll(() => {
  // jsdom'da nesne adresi yok; önizleme yalnızca adresin varlığına bakar.
  URL.createObjectURL = () => 'blob:etiketler';
  URL.revokeObjectURL = () => undefined;
});

describe('Çuval ve etiket ekranı', () => {
  it('kilitli partiye çuval üretir, yeni çuvalları seçer ve etiketlerini basar', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/cuvallar');
    expect(await screen.findByRole('combobox', { name: 'Parti' })).toHaveValue(
      'P-2026-0001 · Kalite A · 1 çuval',
    );
    const count = screen.getByRole('textbox', { name: 'Adet' });
    await userEvent.clear(count);
    await userEvent.type(count, '3');
    await userEvent.click(screen.getByRole('button', { name: 'Çuval oluştur' }));

    expect(await screen.findByRole('status')).toHaveTextContent(/^3 çuval oluşturuldu/);
    expect(await screen.findByText('3 çuval seçili')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getAllByText('Basılmadı')).toHaveLength(3);
    });
    expect(screen.queryByLabelText(/Yeniden baskı gerekçesi/)).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Etiketleri bas' }));
    const dialog = await screen.findByRole('dialog', { name: 'Etiketler' });
    expect(within(dialog).getByTitle('Etiketler')).toHaveAttribute('src', 'blob:etiketler');
    expect(within(dialog).getByRole('link', { name: "PDF'i indir" })).toHaveAttribute(
      'download',
      'cuval-etiketleri.pdf',
    );
    await waitFor(() => {
      expect(screen.getAllByText('1 kez')).toHaveLength(4);
    });
  });

  it('daha önce basılmış etiketi yalnızca gerekçeyle yeniden basar', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/cuvallar');
    await userEvent.click(await screen.findByRole('checkbox', { name: `${PRINTED_KEY} seç` }));
    const print = screen.getByRole('button', { name: 'Etiketleri bas' });
    expect(print).toBeDisabled();

    await userEvent.type(screen.getByLabelText(/Yeniden baskı gerekçesi/), 'Etiket yırtıldı');
    await userEvent.click(print);

    expect(await screen.findByRole('dialog', { name: 'Etiketler' })).toBeInTheDocument();
    expect(await screen.findByText('2 kez')).toBeInTheDocument();
  });

  it('gerekçe boşlukla geçilirse yeniden baskı düğmesi kapalı kalır', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    renderApp('/panel/cuvallar');
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Tümünü seç' }));
    await userEvent.type(screen.getByLabelText(/Yeniden baskı gerekçesi/), ' ');
    expect(screen.getByRole('button', { name: 'Etiketleri bas' })).toBeDisabled();
  });

  it('yetkisi olmayan rol menüde ekranı görmez ve adrese gidince engellenir', async () => {
    server.use(...createHandlers('QUALITY'));
    const { router } = renderApp('/panel/cuvallar');
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/403');
    });
  });
});
