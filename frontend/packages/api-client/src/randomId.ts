/* eslint-disable @typescript-eslint/no-magic-numbers -- RFC 4122 bayt ve bit konumları */
const UUID_BYTES = 16;
const HEX_RADIX = 16;
/** RFC 4122: 6. bayt sürümü (4), 8. bayt türü taşır; bayt sırası → [korunacak bitler, eklenecek bitler]. */
const FIXED_BITS = new Map<number, readonly [number, number]>([
  [6, [0x0f, 0x40]],
  [8, [0x3f, 0x80]],
]);
const HYPHEN_BEFORE = new Set([4, 6, 8, 10]);

/**
 * Rastgele UUID (v4). `crypto.randomUUID` yalnızca güvenli bağlamda (https ya da localhost) vardır; uygulama düz
 * http ile IP adresinden açıldığında (deneme sunucusu) her istek hata verirdi. `crypto.getRandomValues` her yerde vardır.
 */
export function randomId(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(UUID_BYTES));
  return Array.from(bytes, (byte, index) => {
    const [keep, set] = FIXED_BITS.get(index) ?? [0xff, 0];
    const hex = ((byte & keep) | set).toString(HEX_RADIX).padStart(2, '0');
    return (HYPHEN_BEFORE.has(index) ? '-' : '') + hex;
  }).join('');
}
/* eslint-enable @typescript-eslint/no-magic-numbers */
