import Big from 'big.js';

/** API'deki kilo biçimi (Java API-03): nokta ondalıklı, en fazla 3 haneli dize, ör. "1982.500". */
export type Kilogram = `${number}` & { readonly __brand: 'Kilogram' };

const KG_SCALE = 3;
const API_PATTERN = /^\d+(\.\d{1,3})?$/;
const TURKISH_INPUT_PATTERN = /^\d{1,3}(\.\d{3})*(,\d{1,3})?$|^\d+(,\d{1,3})?$/;
const KG_FORMAT = new Intl.NumberFormat('tr-TR', {
  minimumFractionDigits: 0,
  maximumFractionDigits: KG_SCALE,
});

export function isKilogram(value: string): value is Kilogram {
  return API_PATTERN.test(value);
}

/** Negatif değer kilo olamaz; undefined döner. */
function toKilogram(value: Big): Kilogram | undefined {
  const text = value.toFixed(KG_SCALE);
  return isKilogram(text) ? text : undefined;
}

/** Kullanıcının Türkçe girdiği değeri ("1.982,5") API biçimine çevirir; geçersizse undefined. */
export function parseKg(input: string): Kilogram | undefined {
  const trimmed = input.trim();
  if (!TURKISH_INPUT_PATTERN.test(trimmed)) {
    return undefined;
  }
  return toKilogram(new Big(trimmed.replaceAll('.', '').replace(',', '.')));
}

export function formatKg(value: Kilogram): string {
  return `${KG_FORMAT.format(value)} kg`;
}

export function subtractKg(minuend: Kilogram, subtrahend: Kilogram): Big {
  return new Big(minuend).minus(subtrahend);
}

/** Rapor 9.1: net = brüt − dara. Dara bruttan büyükse net hesaplanmaz. */
export function calculateNetKg(grossKg: Kilogram, tareKg: Kilogram): Kilogram | undefined {
  return toKilogram(subtractKg(grossKg, tareKg));
}

/** Reçete satırlarının toplamı ("35016.000"); geçersiz dizeler sayılmaz, satır yoksa 0. */
export function sumKg(values: readonly string[]): string {
  const total = values
    .filter((value) => isKilogram(value))
    .reduce((sum, value) => sum.plus(value), new Big(0));
  return total.toFixed(KG_SCALE);
}

/** API'den gelen kilo dizesini biçimler; sözleşme dışı bir değer olduğu gibi gösterilir. */
export function formatKgText(value: string): string {
  return isKilogram(value) ? formatKg(value) : value;
}

const SIGNED_KG_FORMAT = new Intl.NumberFormat('tr-TR', {
  minimumFractionDigits: 0,
  maximumFractionDigits: KG_SCALE,
  signDisplay: 'exceptZero',
});

/** İşaretli fark ("-12.500" → "-12,5 kg"); sunucudan gelen dize sayı değilse olduğu gibi döner. */
export function formatSignedKg(value: string): string {
  return /^-?\d+(\.\d+)?$/.test(value) ? `${SIGNED_KG_FORMAT.format(Number(value))} kg` : value;
}

/** Kilo dizesi sıfır mı? Sayı olmayan değer sıfır sayılmaz (uyarı tarafında kalmak için). */
export function isZeroKg(value: string): boolean {
  return /^-?\d+(\.\d+)?$/.test(value) && new Big(value).eq(0);
}
