import Big from 'big.js';
const KG_SCALE = 3;
const KG_TEXT = /^-?\d+(\.\d+)?$/;
/**
 * Çıkış tartısının beklenen net ile karşılaştırması (sunucudaki `WeightCheck` ile aynı kural): fark mutlak değeri
 * toleransı aşıyorsa uyuşmazlık; net beklenenin altındaysa eksik kilo nedeni gerekir. Ekranda uyarı için kullanılır,
 * asıl karar sunucudadır. Sayı olmayan girdide undefined döner.
 */
export function compareWeighing(measuredKg, expectedKg, toleranceKg) {
    if (!KG_TEXT.test(measuredKg) || !KG_TEXT.test(expectedKg))
        return undefined;
    const diff = new Big(measuredKg).minus(expectedKg);
    const tolerance = toleranceKg !== undefined && KG_TEXT.test(toleranceKg) ? new Big(toleranceKg) : new Big(0);
    return {
        diffKg: diff.toFixed(KG_SCALE),
        isShort: diff.lt(0),
        exceedsTolerance: diff.abs().gt(tolerance),
    };
}
