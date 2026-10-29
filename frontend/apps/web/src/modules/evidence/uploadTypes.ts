import type { EvidenceType, Permission } from '@iplik/api-client';

/** Kanıt türü → yükleme izni (backend ile aynı eşleme; asıl kontrol backend'dedir). */
export const UPLOAD_PERMISSION_BY_TYPE: Readonly<Record<EvidenceType, Permission>> = {
  DISPATCH_SCALE: 'EVIDENCE_UPLOAD_DISPATCH',
  BAG_OVERVIEW: 'EVIDENCE_UPLOAD_DISPATCH',
  BAG_LABEL: 'EVIDENCE_UPLOAD_DISPATCH',
  SEAL: 'EVIDENCE_UPLOAD_DISPATCH',
  VEHICLE_PLATE: 'EVIDENCE_UPLOAD_DRIVER',
  DELIVERY_SCALE: 'EVIDENCE_UPLOAD_DELIVERY',
  DELIVERY_BAGS: 'EVIDENCE_UPLOAD_DELIVERY',
  SIGNED_DELIVERY_DOC: 'EVIDENCE_UPLOAD_DELIVERY',
  QUALITY_CLAIM: 'EVIDENCE_UPLOAD_CLAIM',
  LAB_RESULT: 'EVIDENCE_UPLOAD_CLAIM',
};

export const ACCEPTED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/heic', 'application/pdf'];
export const ACCEPT = ACCEPTED_MIME_TYPES.join(',');
const MAX_SIZE_MB = 20;
const BYTES_PER_MB = 1_048_576;
export const MAX_SIZE_BYTES = MAX_SIZE_MB * BYTES_PER_MB;

/** Kantar fişi yalnızca fotoğraftır; PDF kabul edilmez. */
export const PHOTO_MIME_TYPES = ACCEPTED_MIME_TYPES.filter((type) => type.startsWith('image/'));

/** Bazı tarayıcılar HEIC dosyasının türünü boş bırakır; uzantıdan tanınır. */
const HEIC_EXTENSION = /\.hei[cf]$/i;

/** i18n anahtarı (`evidence.errors.*`). `accept` yalnızca ipucudur; dosya burada denetlenir (H-20). */
export type FileProblem = 'tooLarge' | 'wrongType';

export function fileProblem(file: File, types: readonly string[]): FileProblem | undefined {
  const isHeic = file.type === '' && HEIC_EXTENSION.test(file.name);
  const isAccepted = types.includes(file.type) || (isHeic && types.includes('image/heic'));
  if (!isAccepted) return 'wrongType';
  return file.size > MAX_SIZE_BYTES ? 'tooLarge' : undefined;
}
