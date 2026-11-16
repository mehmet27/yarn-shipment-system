import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { LOT_OPTIONS_KEY } from './useLots';
import { useSetLotActive } from './useLotMutations';

vi.mock('@iplik/api-client', async (importOriginal) => ({
  ...(await importOriginal<object>()),
  deactivateLot: vi.fn().mockResolvedValue(undefined),
  activateLot: vi.fn().mockResolvedValue(undefined),
}));

describe('useLotMutations', () => {
  it('parti değişince sevkiyat formunun lot seçici önbelleği de eskir', async () => {
    const client = new QueryClient();
    client.setQueryData([...LOT_OPTIONS_KEY, '', undefined], [{ value: 'a', label: 'P-1' }]);
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => useSetLotActive(), { wrapper });
    await act(async () => {
      await result.current.mutateAsync({ id: 'lot-1', isActive: false });
    });
    await waitFor(() => {
      expect(client.getQueryState([...LOT_OPTIONS_KEY, '', undefined])?.isInvalidated).toBe(true);
    });
  });
});
