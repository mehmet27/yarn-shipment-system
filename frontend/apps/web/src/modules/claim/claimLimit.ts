import type { Kilogram } from '@iplik/units';
import { isKilogram, parseKg, subtractKg } from '@iplik/units';

/** Sunucunun dönen sevkiyat net kilosu geçerliyse sınır olarak kullanılır. */
export function limitOf(shipmentNetKg: string): Kilogram | undefined {
  return isKilogram(shipmentNetKg) ? shipmentNetKg : undefined;
}

/** İtiraz edilen kilo sevkiyatın net kilosunu aşıyor mu? Sınır bilinmiyorsa false. */
export function exceedsShipmentKg(claimedKg: Kilogram, limit: Kilogram | undefined): boolean {
  return limit !== undefined && subtractKg(claimedKg, limit).gt(0);
}

const isZero = (kg: string) => Number(kg) === 0;

/** Yazılan itiraz kilosunun durumu: geçerli kilo, biçim hatası ve sevkiyat sınırı aşımı. */
export function claimKgState(kgText: string, shipmentNetKg: string) {
  const claimedKg = parseKg(kgText);
  const limit = limitOf(shipmentNetKg);
  const isZeroOrUnreadable = claimedKg === undefined || isZero(claimedKg);
  return {
    claimedKg,
    limit,
    isInvalid: kgText.trim() !== '' && isZeroOrUnreadable,
    isOverLimit: claimedKg !== undefined && exceedsShipmentKg(claimedKg, limit),
    isUsable: !isZeroOrUnreadable,
  };
}
