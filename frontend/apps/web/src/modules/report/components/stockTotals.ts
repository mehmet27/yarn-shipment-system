import type { BalancesView } from '@iplik/api-client/settlements';
import type { YarnVsFabricRowView } from '@iplik/api-client';
import { sumKg } from '@iplik/units';

/** Fasoncularda yolda olan toplam iplik; veri henüz gelmediyse `undefined`. */
export function sumInTransitKg(balances: BalancesView | undefined): string | undefined {
  return balances === undefined ? undefined : sumKg(balances.items.map((row) => row.inTransitKg));
}

/** Seçilen dönemde fasoncudan gelen toplam kumaş; veri henüz gelmediyse `undefined`. */
export function sumFabricKg(rows: readonly YarnVsFabricRowView[] | undefined): string | undefined {
  return rows === undefined ? undefined : sumKg(rows.map((row) => row.fabricKg));
}
