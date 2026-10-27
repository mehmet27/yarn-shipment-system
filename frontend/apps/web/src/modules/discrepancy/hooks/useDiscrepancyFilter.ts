import type { DiscrepancyStatus as Status, Severity as Level } from '@iplik/api-client';
import { DiscrepancyStatus, Severity } from '@iplik/api-client';
import { useSearchParams } from 'react-router';

export const PAGE_SIZE = 20;

export type DiscrepancyFilter = {
  page: number;
  status: Status | undefined;
  severity: Level | undefined;
  setPage: (page: number) => void;
  setStatus: (status: Status | undefined) => void;
  setSeverity: (severity: Level | undefined) => void;
};

/** FE-API-08: filtre ve sayfa URL'de tutulur; sayfa URL'de 1'den başlar. */
export function useDiscrepancyFilter(): DiscrepancyFilter {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Math.max(Number(searchParams.get('sayfa') ?? '1'), 1);
  const status = Object.values(DiscrepancyStatus).find((s) => s === searchParams.get('durum'));
  const severity = Object.values(Severity).find((s) => s === searchParams.get('onem'));
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
    severity,
    setPage: (value) => {
      update({ sayfa: String(value) });
    },
    setStatus: (value) => {
      update({ durum: value, sayfa: undefined });
    },
    setSeverity: (value) => {
      update({ onem: value, sayfa: undefined });
    },
  };
}
