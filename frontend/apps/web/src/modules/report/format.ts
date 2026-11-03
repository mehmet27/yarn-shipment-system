import { formatKg, isKilogram } from '@iplik/units';

/** Sunucudan gelen ondalık kilo dizesi; tanınmazsa olduğu gibi gösterilir. */
export function formatKilograms(value: string): string {
  return isKilogram(value) ? formatKg(value) : `${value} kg`;
}

/** Yüzde dizesi, ör. `"25.00"` → `%25,00`. */
export function formatPercent(value: string): string {
  return `%${value.replace('.', ',')}`;
}

const SHORT_ID_LENGTH = 8;

/** Adı çözülmemiş şoför, fasoncu kimlikleri kısaltılarak gösterilir. */
export function shortId(id: string): string {
  return id.slice(0, SHORT_ID_LENGTH);
}
