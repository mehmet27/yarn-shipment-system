import { UiProvider } from '@iplik/ui';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { appRoutes } from '../app/router';

/** Uygulamayı gerçek route'larla, verilen adreste ve tekrar denemesiz sorgu istemcisiyle açar. */
export function renderApp(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });
  render(
    <UiProvider env="test">
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </UiProvider>,
  );
  return { router };
}
