import Big from 'big.js';
const KG_SCALE = 3;
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
    return toKilogram(new Big(trimmed.replaceAll('.', '').replace(',', '.')));
}
export function formatKg(value) {
    return `${KG_FORMAT.format(value)} kg`;
}
export function subtractKg(minuend, subtrahend) {
    return new Big(minuend).minus(subtrahend);
}
/** Rapor 9.1: net = brüt − dara. Dara bruttan büyükse net hesaplanmaz. */
export function calculateNetKg(grossKg, tareKg) {
    return toKilogram(subtractKg(grossKg, tareKg));
}
