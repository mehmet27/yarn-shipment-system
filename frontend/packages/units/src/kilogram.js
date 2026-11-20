import Big from 'big.js';
const KG_SCALE = 3;
/** Tek kayıtta girilebilecek en büyük kilo (backend `Kilograms.MAX_ENTRY_KG` ile aynı). */
const MAX_ENTRY_KG = 100_000;
const API_PATTERN = /^\d+(\.\d{1,3})?$/;
const TURKISH_INPUT_PATTERN = /^\d{1,3}(\.\d{3})*(,\d{1,3})?$|^\d+(,\d{1,3})?$/;
const KG_FORMAT = new Intl.NumberFormat('tr-TR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: KG_SCALE,
});
export function isKilogram(value) {
    return API_PATTERN.test(value);
}
/** Negatif değer kilo olamaz; undefined döner. */
function toKilogram(value) {
    const text = value.toFixed(KG_SCALE);
    return isKilogram(text) ? text : undefined;
}
/** Kullanıcının Türkçe girdiği değeri ("1.982,5") API biçimine çevirir; geçersizse undefined. */
export function parseKg(input) {
    const trimmed = input.trim();
    if (!TURKISH_INPUT_PATTERN.test(trimmed)) {
        return undefined;
    }
    const value = new Big(trimmed.replaceAll('.', '').replace(',', '.'));
    return value.gt(MAX_ENTRY_KG) ? undefined : toKilogram(value);
}
export function formatKg(value) {
    return `${KG_FORMAT.format(value)} kg`;
}
/**
 * Ekrandaki, PDF'teki ve Excel yazdırmasındaki tek kilo biçimi: binlik nokta, ondalık virgül,
 * gereksiz sıfır yok ("2.143,814 kg", "264,5 kg", "700 kg"). Sayı ya da sayı dizesi alır.
 */
export function formatKgNumber(value) {
    return `${KG_FORMAT.format(Number(value))} kg`;
}
export function subtractKg(minuend, subtrahend) {
    return new Big(minuend).minus(subtrahend);
}
/** Rapor 9.1: net = brüt − dara. Dara bruttan büyükse net hesaplanmaz. */
export function calculateNetKg(grossKg, tareKg) {
    return toKilogram(subtractKg(grossKg, tareKg));
}
/** Reçete satırlarının toplamı ("35016.000"); geçersiz dizeler sayılmaz, satır yoksa 0. */
export function sumKg(values) {
    const total = values
        .filter((value) => isKilogram(value))
        .reduce((sum, value) => sum.plus(value), new Big(0));
    return total.toFixed(KG_SCALE);
}
const SIGNED_API_PATTERN = /^-?\d+(\.\d{1,3})?$/;
/**
 * İşaretli kilo dizelerinin toplamı ("-35.250" dahil), ör. bakiye ve stok satırları; `sumKg` eksi değeri saymaz.
 * Sayı olmayan dizeler sayılmaz, satır yoksa "0.000".
 */
export function sumSignedKg(values) {
    const total = values
        .filter((value) => SIGNED_API_PATTERN.test(value))
        .reduce((sum, value) => sum.plus(value), new Big(0));
    return total.toFixed(KG_SCALE);
}
/** API'den gelen kilo dizesini biçimler; sözleşme dışı bir değer olduğu gibi gösterilir. */
export function formatKgText(value) {
    const plain = value.replace(/\s*kg$/i, '').trim();
    if (isKilogram(plain))
        return formatKg(plain);
    const magnitude = plain.startsWith('-') ? plain.slice(1) : undefined;
    return magnitude !== undefined && isKilogram(magnitude) ? `−${formatKg(magnitude)}` : value;
}
const SIGNED_KG_FORMAT = new Intl.NumberFormat('tr-TR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: KG_SCALE,
    signDisplay: 'exceptZero',
});
/** İşaretli fark ("-12.500" → "-12,5 kg"); sunucudan gelen dize sayı değilse olduğu gibi döner. */
export function formatSignedKg(value) {
    return /^-?\d+(\.\d+)?$/.test(value) ? `${SIGNED_KG_FORMAT.format(Number(value))} kg` : value;
}
/** Kilo dizesi sıfır mı? Sayı olmayan değer sıfır sayılmaz (uyarı tarafında kalmak için). */
export function isZeroKg(value) {
    return /^-?\d+(\.\d+)?$/.test(value) && new Big(value).eq(0);
}
