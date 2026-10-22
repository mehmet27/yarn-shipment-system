export const MANAGED_KINDS = ['subcontractor', 'driver', 'vehicle', 'site'] as const;
export type ManagedKind = (typeof MANAGED_KINDS)[number];

export const MASTER_ADMIN_PATH = '/api/v1/master-data';

/** Ekranda her türün listesi ve formu aynı biçimdedir; `label` ünvan, ad-soyad, plaka veya lokasyon adı. */
export type RecordRow = {
  id: string;
  label: string;
  isActive: boolean;
  subcontractorId?: string;
};

export type RecordValues = { label: string; subcontractorId?: string };

export type RecordPage = { rows: RecordRow[]; totalPages: number };

export type ListParams = {
  q: string;
  page: number;
  size: number;
  isShowingInactive: boolean;
};
