import { formatKgText, isKilogram } from '@iplik/units';

/** Sunucudan gelen ondalık kilo dizesi; tanınmazsa olduğu gibi gösterilir. */
export function formatKilograms(value: string): string {
  return isKilogram(value) || value.startsWith('-') ? formatKgText(value) : `${value} kg`;
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

const kg3 = new Intl.NumberFormat('tr-TR', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
const pct2 = new Intl.NumberFormat('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Üç ondalıklı Türkçe kilo, ör. `1.982,500 kg`. */
export function formatKg3(value: string | number): string {
  return `${kg3.format(Number(value))} kg`;
}

/** Sunucu oranı kesir (0,04) ya da yüzde olabilir; fire oranı kesir olarak gelir: `0.0403` → `%4,03`. */
export function formatFireRate(value: string | number): string {
  return `%${pct2.format(Number(value) * 100)}`;
}
