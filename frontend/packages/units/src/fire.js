import Big from 'big.js';
export { sumKg } from './kilogram';
const SCALE = 3;
/**
 * Analiz Bölüm 8.1: T=G-D-I, B=T*(1-f), E=B-(K-I), H=max(0,E), fire=(T-(K-I))/T. Yarım yukarı yuvarlama.
 * Taban sıfır ya da negatifse fire yüzdesi 0 döner. Yalnızca sahte API ve önizleme içindir; asıl hesap backend'dedir.
 */
export function calculateFire(input) {
    const base = new Big(input.sentKg).minus(input.returnedKg).minus(input.claimKg);
    const expected = base.times(new Big(1).minus(input.fireRate));
    const received = new Big(input.fabricKg).minus(input.claimKg);
    const shortage = expected.minus(received);
    const pct = base.gt(0) ? base.minus(received).div(base).times(100) : new Big(0);
    return {
        baseKg: base.toFixed(SCALE),
        expectedFabricKg: expected.toFixed(SCALE),
        shortageKg: shortage.toFixed(SCALE),
        faultyKg: (shortage.gt(0) ? shortage : new Big(0)).toFixed(SCALE),
        actualFirePercent: pct.toFixed(2),
    };
}
