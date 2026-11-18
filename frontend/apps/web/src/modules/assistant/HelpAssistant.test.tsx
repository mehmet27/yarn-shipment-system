import { ASSISTANT_PATH } from '@iplik/api-client/assistant';
import { UiProvider } from '@iplik/ui';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '@/mocks/server';
import { HelpAssistant } from './HelpAssistant';

function renderAssistant() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <UiProvider env="test">
      <QueryClientProvider client={queryClient}>
        <HelpAssistant />
      </QueryClientProvider>
    </UiProvider>,
  );
}

function answerStatus(available: boolean) {
  server.use(http.get(`${ASSISTANT_PATH}/status`, () => HttpResponse.json({ available })));
}

describe('HelpAssistant', () => {
  it('sunucuda anahtar yoksa balon görünmez', async () => {
    answerStatus(false);
    renderAssistant();

    await new Promise((resolve) => setTimeout(resolve, 50));

    expect(screen.queryByRole('button', { name: 'Yardım' })).not.toBeInTheDocument();
  });

  it('soruyu önceki mesajlarla birlikte sorar ve cevabı gösterir', async () => {
    answerStatus(true);
    const bodies: unknown[] = [];
    server.use(
      http.post(`${ASSISTANT_PATH}/ask`, async ({ request }) => {
        bodies.push(await request.json());
        return HttpResponse.json({
          answer: bodies.length === 1 ? 'Birinci cevap' : 'İkinci cevap',
        });
      }),
    );
    renderAssistant();

    await userEvent.click(await screen.findByRole('button', { name: 'Yardım' }));
    await userEvent.type(
      screen.getByRole('textbox', { name: 'Sorunuzu yazın' }),
      'Çuval nasıl eklenir?{Enter}',
    );

    expect(await screen.findByText('Birinci cevap')).toBeInTheDocument();
    await userEvent.type(
      screen.getByRole('textbox', { name: 'Sorunuzu yazın' }),
      'Peki silinir mi?{Enter}',
    );
    expect(await screen.findByText('İkinci cevap')).toBeInTheDocument();
    expect(bodies[0]).toEqual({ question: 'Çuval nasıl eklenir?', history: [] });
    expect(bodies[1]).toEqual({
      question: 'Peki silinir mi?',
      history: [
        { role: 'user', text: 'Çuval nasıl eklenir?' },
        { role: 'assistant', text: 'Birinci cevap' },
      ],
    });
  });

  it('soru sınırı dolunca bekleme uyarısı gösterir', async () => {
    answerStatus(true);
    server.use(
      http.post(`${ASSISTANT_PATH}/ask`, () =>
        HttpResponse.json({ status: 429, code: 'HELP_RATE_LIMITED', title: 'x' }, { status: 429 }),
      ),
    );
    renderAssistant();

    await userEvent.click(await screen.findByRole('button', { name: 'Yardım' }));
    await userEvent.type(screen.getByRole('textbox', { name: 'Sorunuzu yazın' }), 'Merhaba{Enter}');

    expect(await screen.findByRole('alert')).toHaveTextContent('Kısa sürede çok soru sordunuz');
  });

  it('cevap alınamazsa tekrar denemesini söyler', async () => {
    answerStatus(true);
    server.use(
      http.post(`${ASSISTANT_PATH}/ask`, () =>
        HttpResponse.json({ status: 503, code: 'HELP_UNAVAILABLE', title: 'x' }, { status: 503 }),
      ),
    );
    renderAssistant();

    await userEvent.click(await screen.findByRole('button', { name: 'Yardım' }));
    await userEvent.type(screen.getByRole('textbox', { name: 'Sorunuzu yazın' }), 'Merhaba{Enter}');

    expect(await screen.findByRole('alert')).toHaveTextContent('Şu an cevap veremiyorum');
  });
});
