import { useSearchParams } from 'react-router';

export const AUDIT_PAGE_SIZE = 20;
export const ENTITY_TYPES = ['Shipment', 'Evidence', 'Discrepancy'] as const;
export type EntityType = (typeof ENTITY_TYPES)[number];

export type AuditFilter = {
  page: number;
  entityType: EntityType | undefined;
  action: string;
  setPage: (page: number) => void;
  setEntityType: (entityType: EntityType | undefined) => void;
  setAction: (action: string) => void;
};

/** Filtre ve sayfa URL'de tutulur; sayfa URL'de 1'den başlar. */
export function useAuditFilter(): AuditFilter {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Math.max(Number(searchParams.get('sayfa') ?? '1'), 1);
  const entityType = ENTITY_TYPES.find((type) => type === searchParams.get('varlik'));
  const action = searchParams.get('islem') ?? '';
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
    page,
    entityType,
    action,
    setPage: (value) => {
      update({ sayfa: String(value) });
    },
    setEntityType: (value) => {
      update({ varlik: value, sayfa: undefined });
    },
    setAction: (value) => {
      update({ islem: value, sayfa: undefined });
    },
  };
}
