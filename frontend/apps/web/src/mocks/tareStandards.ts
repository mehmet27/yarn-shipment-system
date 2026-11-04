import type { TareStandardView } from '@iplik/api-client';

let standards: TareStandardView[] = [];

export function listTareStandards(siteId: string | null): readonly TareStandardView[] {
  return standards.filter((row) => siteId === null || row.siteId === siteId);
}

/** Lokasyon ve ambalaj başına tek kayıt; varsa değerleri güncellenir (PUT). */
export function saveTareStandard(
  site: { id: string; label: string },
  values: Pick<TareStandardView, 'packagingType' | 'standardTareKg' | 'maxDeviationPct'>,
): TareStandardView {
  const { packagingType, standardTareKg, maxDeviationPct } = values;
  const { id: siteId, label: siteName } = site;
  const existing = standards.find(
    (row) => row.siteId === siteId && row.packagingType === packagingType,
  );
  const saved: TareStandardView = {
    id: existing?.id ?? `tare-${String(standards.length + 1)}`,
    siteId,
    siteName,
    packagingType,
    standardTareKg,
    maxDeviationPct,
  };
  standards =
    existing === undefined
      ? [...standards, saved]
      : standards.map((row) => (row === existing ? saved : row));
  return saved;
}

export function resetTareStandards(): void {
  standards = [];
}
