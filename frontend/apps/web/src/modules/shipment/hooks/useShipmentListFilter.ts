import type { ShipmentStatus } from '@iplik/api-client';
import { SHIPMENT_STATUSES } from '@iplik/api-client';
import { useSearchParams } from 'react-router';
import { useRememberedView } from '@/modules/shared';

export const PAGE_SIZE = 20;

/** Kişinin son seçtiği durum hatırlanır; arama metni, tarih aralığı ve sayfa numarası hatırlanmaz. */
const REMEMBERED_KEYS = ['durum'] as const;
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export type ShipmentListFilter = {
  page: number;
  status: ShipmentStatus | undefined;
  /** Sevkiyat no, fasoncu, şoför ya da plakada aranan metin; yoksa boş. */
  search: string;
  /** Sevkiyat tarihi aralığı (ISO gün); verilmediyse boş. */
  from: string;
  to: string;
  hasRangeError: boolean;
  isFiltered: boolean;
  setPage: (page: number) => void;
  setStatus: (status: ShipmentStatus | undefined) => void;
  setSearch: (search: string) => void;
  setRange: (range: { from: string; to: string }) => void;
  clear: () => void;
};

function parseStatus(value: string | null): ShipmentStatus | undefined {
  return SHIPMENT_STATUSES.find((status) => status === value);
}

function parseDate(value: string | null): string {
  return value !== null && ISO_DATE.test(value) ? value : '';
}

/** FE-API-08: filtre ve sayfa URL'de tutulur; link paylaşılabilir. Sayfa URL'de 1'den başlar. */
export function useShipmentListFilter(): ShipmentListFilter {
  const [searchParams, setSearchParams] = useSearchParams();
  useRememberedView('sevkiyatlar', REMEMBERED_KEYS);
  const status = parseStatus(searchParams.get('durum'));
  const search = (searchParams.get('ara') ?? '').trim();
  const from = parseDate(searchParams.get('bas'));
  const to = parseDate(searchParams.get('bit'));

  const update = (changes: Record<string, string | undefined>) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current);
      Object.entries(changes).forEach(([key, value]) => {
        if (value === undefined || value === '') next.delete(key);
        else next.set(key, value);
      });
      return next;
    });
  };

  return {
    page: Math.max(Number(searchParams.get('sayfa') ?? '1'), 1),
    status,
    search,
    from,
    to,
    hasRangeError: from !== '' && to !== '' && from > to,
    isFiltered: status !== undefined || search !== '' || from !== '' || to !== '',
    setPage: (nextPage) => {
      update({ sayfa: String(nextPage) });
    },
    setStatus: (nextStatus) => {
      update({ durum: nextStatus, sayfa: undefined });
    },
    setSearch: (nextSearch) => {
      update({ ara: nextSearch.trim(), sayfa: undefined });
    },
    setRange: (range) => {
      update({ bas: range.from, bit: range.to, sayfa: undefined });
    },
    clear: () => {
      update({
        durum: undefined,
        ara: undefined,
        bas: undefined,
        bit: undefined,
        sayfa: undefined,
      });
    },
  };
}

/** Süzgecin sunucuya giden karşılığı; boş alan hiç gönderilmez (exactOptionalPropertyTypes). */
export function toListParams(filter: ShipmentListFilter) {
  const hasRange = !filter.hasRangeError;
  return {
    ...(filter.status === undefined ? {} : { status: [filter.status] }),
    ...(filter.search === '' ? {} : { search: filter.search }),
    ...(hasRange && filter.from !== '' ? { from: filter.from } : {}),
    ...(hasRange && filter.to !== '' ? { to: filter.to } : {}),
  };
}
