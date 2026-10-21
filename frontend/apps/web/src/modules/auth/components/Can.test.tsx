import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { createMeHandler } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { Can } from './Can';

function renderCan() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <Can permission="UYUSMAZLIK_KARAR" fallback={<span>yetkisiz</span>}>
        <button type="button">Karar ver</button>
      </Can>
    </QueryClientProvider>,
  );
}

describe('Can', () => {
  it('izni olan kullanıcıya içeriği gösterir', async () => {
    server.use(createMeHandler('KALITE'));
    renderCan();
    expect(await screen.findByRole('button', { name: 'Karar ver' })).toBeInTheDocument();
  });

  it('izni olmayan kullanıcıya yedek içeriği gösterir', async () => {
    server.use(createMeHandler('YONETICI'));
    renderCan();
    expect(await screen.findByText('yetkisiz')).toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
