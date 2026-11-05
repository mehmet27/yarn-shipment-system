import { ErrorBoundary } from '@iplik/ui';
import { QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { RouterProvider } from 'react-router';
import { BrandedUiProvider } from '@/modules/branding';
import { CrashPage } from './pages/CrashPage';
import { createQueryClient } from './queryClient';
import { createAppRouter } from './router';

export function App() {
  const [queryClient] = useState(createQueryClient);
  const [router] = useState(createAppRouter);
  return (
    <QueryClientProvider client={queryClient}>
      <BrandedUiProvider>
        <ErrorBoundary fallback={<CrashPage />}>
          <RouterProvider router={router} />
        </ErrorBoundary>
      </BrandedUiProvider>
    </QueryClientProvider>
  );
}
