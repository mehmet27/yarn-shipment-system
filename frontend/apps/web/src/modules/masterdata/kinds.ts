export const MANAGED_KINDS = [
  'subcontractor',
  'driver',
  'vehicle',
  'site',
  'product',
  'qualityGrade',
] as const;
export type ManagedKind = (typeof MANAGED_KINDS)[number];

/** Kodlu türler: kod yalnızca eklerken girilir, sonra değişmez; `extra` iplik numarası ya da etiket rengi. */
export const CODED_KINDS: readonly ManagedKind[] = ['product', 'qualityGrade'];

export function isCodedKind(kind: ManagedKind): boolean {
  return CODED_KINDS.includes(kind);
}

export const MASTER_ADMIN_PATH = '/api/v1/master-data';

/** Ekranda her türün listesi ve formu aynı biçimdedir; `label` ünvan, ad-soyad, plaka, lokasyon, iplik ya da kalite adı. */
export type RecordRow = {
  id: string;
  label: string;
  isActive: boolean;
  subcontractorId?: string;
  ownerTitle?: string;
  code?: string;
  extra?: string;
};

export type RecordValues = {
  label: string;
  subcontractorId?: string;
  code?: string;
  extra?: string;
};

export type RecordPage = { rows: RecordRow[]; totalPages: number };

export type ListParams = {
  q: string;
  page: number;
  size: number;
  isShowingInactive: boolean;
};
