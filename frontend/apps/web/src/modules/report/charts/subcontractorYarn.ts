import type { BalancesView } from '@iplik/api-client/settlements';
import { sumSignedKg } from '@iplik/units';

export type SubcontractorYarn = {
  subcontractorId: string;
  subcontractorTitle: string;
  /** Fasoncuda kalan iplik (iplik cinslerinin toplamı). */
  balanceKg: string;
  /** Bunun henüz yolda olan kısmı. */
  inTransitKg: string;
};

/** Bakiye satırlarını (fasoncu × iplik cinsi) fasoncu başına toplar; eksi satırlar da toplama girer. */
export function sumBySubcontractor(balances: BalancesView): SubcontractorYarn[] {
  const groups = new Map<string, BalancesView['items']>();
  for (const row of balances.items) {
    groups.set(row.subcontractorId, [...(groups.get(row.subcontractorId) ?? []), row]);
  }
  return [...groups].map(([subcontractorId, rows]) => ({
    subcontractorId,
    subcontractorTitle: rows[0]?.subcontractorTitle ?? '',
    balanceKg: sumSignedKg(rows.map((row) => row.balanceKg)),
    inTransitKg: sumSignedKg(rows.map((row) => row.inTransitKg)),
  }));
}
