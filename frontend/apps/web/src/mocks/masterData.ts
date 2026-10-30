export type MasterKind = 'subcontractor' | 'driver' | 'vehicle' | 'site';

/** Dört ana veri türünün ortak sahte kaydı; `label` ünvan, ad-soyad, plaka veya lokasyon adıdır. */
export type MockRecord = {
  id: string;
  label: string;
  isActive: boolean;
  subcontractorId?: string;
};

export type MockValues = { label: string; subcontractorId?: string };

const SEED: Readonly<Record<MasterKind, readonly MockRecord[]>> = {
  subcontractor: [
    { id: 'sub-0', label: 'Aydın Örme', isActive: true },
    { id: 'sub-1', label: 'Bursa Tekstil', isActive: true },
    { id: 'sub-2', label: 'Çorlu Boya', isActive: true },
    { id: 'sub-3', label: 'Eski Dokuma', isActive: false },
  ],
  driver: [
    { id: 'drv-0', label: 'Şahin Şoför', isActive: true, subcontractorId: 'sub-0' },
    { id: 'drv-1', label: 'Mert Kaya', isActive: true, subcontractorId: 'sub-1' },
    { id: 'drv-2', label: 'Ali Demir', isActive: true, subcontractorId: 'sub-1' },
  ],
  vehicle: [
    { id: 'veh-0', label: '34ABC123', isActive: true },
    { id: 'veh-1', label: '16XYZ456', isActive: true },
  ],
  site: [{ id: 'site-merkez', label: 'Merkez Depo', isActive: true }],
};

let records = structuredClone(SEED) as Record<MasterKind, MockRecord[]>;
let nextId = 1;

export function listMasterRecords(kind: MasterKind): readonly MockRecord[] {
  return records[kind];
}

/** Plaka sunucuda boşluksuz büyük harfe çevrilir. */
function normalize(kind: MasterKind, label: string): string {
  const trimmed = label.trim();
  return kind === 'vehicle' ? trimmed.replaceAll(/\s+/g, '').toUpperCase() : trimmed;
}

/** Sunucudaki TR plaka deseni (backend ile aynı). */
const PLATE_PATTERN = /^(0[1-9]|[1-7]\d|8[01])[A-Z]{1,3}\d{2,4}$/;

export function isValidPlate(label: string): boolean {
  return PLATE_PATTERN.test(normalize('vehicle', label));
}

export function isDuplicatePlate(label: string, exceptId?: string): boolean {
  const plate = normalize('vehicle', label);
  return records.vehicle.some((record) => record.label === plate && record.id !== exceptId);
}

/** Fasoncu ünvanı büyük/küçük harf ve kenar boşluğu gözetmeden tekildir. */
export function isDuplicateTitle(label: string, exceptId?: string): boolean {
  const title = label.trim().toLocaleLowerCase('tr');
  return records.subcontractor.some(
    (record) => record.label.toLocaleLowerCase('tr') === title && record.id !== exceptId,
  );
}

export function createMasterRecord(kind: MasterKind, values: MockValues): MockRecord {
  const created: MockRecord = {
    id: `${kind}-new-${String(nextId++)}`,
    label: normalize(kind, values.label),
    isActive: true,
    ...(values.subcontractorId === undefined ? {} : { subcontractorId: values.subcontractorId }),
  };
  records[kind] = [...records[kind], created];
  return created;
}

export function updateMasterRecord(
  kind: MasterKind,
  id: string,
  values: MockValues,
): MockRecord | undefined {
  const found = records[kind].find((record) => record.id === id);
  if (found === undefined) return undefined;
  const updated: MockRecord = {
    id: found.id,
    isActive: found.isActive,
    label: normalize(kind, values.label),
    ...(values.subcontractorId === undefined ? {} : { subcontractorId: values.subcontractorId }),
  };
  records[kind] = records[kind].map((record) => (record.id === id ? updated : record));
  return updated;
}

export function setMasterActive(kind: MasterKind, id: string, active: boolean): boolean {
  if (!records[kind].some((record) => record.id === id)) return false;
  records[kind] = records[kind].map((record) =>
    record.id === id ? { ...record, isActive: active } : record,
  );
  return true;
}

export function resetMasterData(): void {
  records = structuredClone(SEED) as Record<MasterKind, MockRecord[]>;
  nextId = 1;
}

/** Arama büyük-küçük harf duyarsızdır (Türkçe yerel ayarla). */
export function matchesQuery(text: string, q: string | null): boolean {
  return q === null || text.toLocaleLowerCase('tr').includes(q.toLocaleLowerCase('tr'));
}
