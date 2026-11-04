import {
  ActionBar,
  ActivityFeed,
  EmptyState,
  ListGroupHeader,
  MiniProgress,
  PageHero,
  ProgressSteps,
  StatCard,
  UiProvider,
} from '@iplik/ui';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

describe('ortak tasarım bileşenleri', () => {
  it('StatCard etiketi, değeri ve ipucunu gösterir', () => {
    render(
      <UiProvider env="test">
        <StatCard label="İşlem bekleyen" value={6} tone="warning" hint="2 tanesi gecikti" />
      </UiProvider>,
    );
    expect(screen.getByText('İşlem bekleyen')).toBeTruthy();
    expect(screen.getByText('6')).toBeTruthy();
    expect(screen.getByText('2 tanesi gecikti')).toBeTruthy();
  });

  it('MiniProgress toplam kadar dilim çizer ve metin etiketi taşır', () => {
    render(
      <UiProvider env="test">
        <MiniProgress total={6} done={3} color="blue" label="3 / 6 adım" />
      </UiProvider>,
    );
    expect(screen.getByRole('img', { name: '3 / 6 adım' })).toBeTruthy();
    expect(screen.getAllByTestId('mini-progress-segment')).toHaveLength(6);
  });

  it('ProgressSteps yalnızca geçerli adımı aria-current ile işaretler', () => {
    render(
      <UiProvider env="test">
        <ProgressSteps steps={['Taslak', 'Yüklendi', 'Yolda']} current={1} label="Süreç" />
      </UiProvider>,
    );
    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items.map((item) => item.getAttribute('aria-current'))).toEqual([null, 'step', null]);
  });

  it('PageHero başlık, alt başlık, eylem ve altına taşan içeriği gösterir', () => {
    render(
      <UiProvider env="test">
        <PageHero title="Sevkiyatlar" subtitle="Tuzla Depo" actions={<button>Yeni</button>}>
          <p>kartlar</p>
        </PageHero>
      </UiProvider>,
    );
    expect(screen.getByRole('heading', { name: 'Sevkiyatlar' })).toBeTruthy();
    expect(screen.getByText('Tuzla Depo')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Yeni' })).toBeTruthy();
    expect(screen.getByText('kartlar')).toBeTruthy();
  });

  it('ListGroupHeader, ActivityFeed, EmptyState ve ActionBar içeriklerini gösterir', () => {
    render(
      <UiProvider env="test">
        <ListGroupHeader label="İşlem bekleyen" count={4} />
        <ActivityFeed
          label="Etkinlik"
          items={[{ id: '1', title: 'Taslak oluşturuldu', meta: '20.10.2026 09:00' }]}
        />
        <EmptyState
          title="Kayıt yok"
          description="Henüz sevkiyat yok"
          action={<a href="/x">Ekle</a>}
        />
        <ActionBar>
          <button>Kaydet</button>
        </ActionBar>
      </UiProvider>,
    );
    expect(screen.getByText('İşlem bekleyen')).toBeTruthy();
    expect(screen.getByText('4')).toBeTruthy();
    expect(screen.getByText('Taslak oluşturuldu')).toBeTruthy();
    expect(screen.getByText('Kayıt yok')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Ekle' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Kaydet' })).toBeTruthy();
  });
});
