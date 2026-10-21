import type { ShipmentStatus } from '@iplik/api-client';
import { SHIPMENT_STATUSES } from '@iplik/api-client';
import { useSearchParams } from 'react-router';

export const PAGE_SIZE = 20;

export type ShipmentListFilter = {
  page: number;
  status: ShipmentStatus | undefined;
  setPage: (page: number) => void;
  setStatus: (status: ShipmentStatus | undefined) => void;
};

function parseStatus(value: string | null): ShipmentStatus | undefined {
  return SHIPMENT_STATUSES.find((status) => status === value);
}

/** FE-API-08: filtre ve sayfa URL'de tutulur; link paylaşılabilir. Sayfa URL'de 1'den başlar. */
export function useShipmentListFilter(): ShipmentListFilter {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Math.max(Number(searchParams.get('sayfa') ?? '1'), 1);
  const status = parseStatus(searchParams.get('durum'));

  const update = (changes: Record<string, string | undefined>) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current);
      Object.entries(changes).forEach(([key, value]) => {
        if (value === undefined) next.delete(key);
        else next.set(key, value);
      });
      return next;
    });
  };

  return {
    page,
    status,
    setPage: (nextPage) => {
      update({ sayfa: String(nextPage) });
    },
    setStatus: (nextStatus) => {
      update({ durum: nextStatus, sayfa: undefined });
    },
  };
}
