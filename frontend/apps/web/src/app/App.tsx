import { ErrorBoundary, UiProvider } from '@iplik/ui';
import { QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { RouterProvider } from 'react-router';
import { CrashPage } from './pages/CrashPage';
import { createQueryClient } from './queryClient';
import { createAppRouter } from './router';

export function App() {
  const [queryClient] = useState(createQueryClient);
  const [router] = useState(createAppRouter);
  return (
    <UiProvider>
      <QueryClientProvider client={queryClient}>
        <ErrorBoundary fallback={<CrashPage />}>
          <RouterProvider router={router} />
        </ErrorBoundary>
      </QueryClientProvider>
    </UiProvider>
  );
}
