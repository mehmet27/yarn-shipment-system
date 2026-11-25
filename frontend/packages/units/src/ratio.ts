const HUNDRED = 100;
const RATIO_PATTERN = /^\d{1,3}([.,]\d{1,2})?$/;

/** Yüzde 100'ün yüzde birlik (0,01) cinsinden değeri; oranlar iki ondalıkla tutulur. */
export const FULL_HUNDREDTHS = 10_000;

/** "33,5" ya da "33.50" → "33.5"; 0'dan büyük, en çok 100 ve en çok iki ondalıklı değilse undefined. */
export function parseRatio(text: string): string | undefined {
  const trimmed = text.trim();
  if (!RATIO_PATTERN.test(trimmed)) return undefined;
  const normalized = trimmed.replace(',', '.');
  const value = Number(normalized);
  return value > 0 && value <= HUNDRED ? String(value) : undefined;
}

/** Yüzdeyi yüzde birlik tam sayıya çevirir; toplamlar kayan nokta hatası olmadan karşılaştırılır. */
export function ratioHundredths(ratio: string): number {
  return Math.round(Number(ratio) * HUNDRED);
}
