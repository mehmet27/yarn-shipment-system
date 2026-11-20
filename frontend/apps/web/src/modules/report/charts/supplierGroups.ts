import type { SupplierReceipt } from '@iplik/api-client/central-stock';
import { sumKg } from '@iplik/units';

export type SupplierGroup = { name: string; rows: SupplierReceipt[]; totalKg: string };

/** Aynı tedarikçinin satırlarını birleştirir; tedarikçi sırası gelen sıradır (sunucu ada göre sıralar). */
export function groupBySupplier(rows: readonly SupplierReceipt[]): SupplierGroup[] {
  const groups = new Map<string, SupplierReceipt[]>();
  for (const row of rows) {
    groups.set(row.supplierName, [...(groups.get(row.supplierName) ?? []), row]);
  }
  return [...groups].map(([name, own]) => ({
    name,
    rows: own,
    totalKg: sumKg(own.map((row) => row.receivedKg)),
  }));
}
