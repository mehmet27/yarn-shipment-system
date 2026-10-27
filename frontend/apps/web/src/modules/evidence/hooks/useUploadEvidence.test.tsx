import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { createHandlers } from '@/mocks/handlers';
import { server } from '@/mocks/server';
import { useUploadEvidence } from './useUploadEvidence';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useUploadEvidence', () => {
  it('izin ister, dosyayı depoya gönderir ve onaylar', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const { result } = renderHook(() => useUploadEvidence('shp-0001'), { wrapper });
    const file = new File(['abc'], 'tarti.jpg', { type: 'image/jpeg' });
    result.current.mutate({ file, evidenceType: 'DISPATCH_SCALE', source: 'GALLERY' });
    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
    expect(result.current.data?.evidenceType).toBe('DISPATCH_SCALE');
  });

  it('izni olmayan kanıt türünü backend reddeder', async () => {
    server.use(...createHandlers('WAREHOUSE'));
    const { result } = renderHook(() => useUploadEvidence('shp-0001'), { wrapper });
    const file = new File(['abc'], 'plaka.jpg', { type: 'image/jpeg' });
    result.current.mutate({ file, evidenceType: 'VEHICLE_PLATE', source: 'CAMERA' });
    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});
