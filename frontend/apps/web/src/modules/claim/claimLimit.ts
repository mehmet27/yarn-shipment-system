import type { Kilogram } from '@iplik/units';
import { isKilogram, parseKg, subtractKg } from '@iplik/units';

/** Sunucunun döndüğü itiraz edilebilir kilo geçerliyse sınır olarak kullanılır. */
export function limitOf(claimableKg: string): Kilogram | undefined {
  return isKilogram(claimableKg) ? claimableKg : undefined;
}

/** İtiraz edilen kilo, itiraz edilebilir kiloyu aşıyor mu? Sınır bilinmiyorsa false. */
export function exceedsShipmentKg(claimedKg: Kilogram, limit: Kilogram | undefined): boolean {
  return limit !== undefined && subtractKg(claimedKg, limit).gt(0);
}

const isZero = (kg: string) => Number(kg) === 0;

/** Yazılan itiraz kilosunun durumu: geçerli kilo, biçim hatası ve itiraz edilebilir kilo aşımı. */
export function claimKgState(kgText: string, claimableKg: string) {
  const claimedKg = parseKg(kgText);
  const limit = limitOf(claimableKg);
  const isZeroOrUnreadable = claimedKg === undefined || isZero(claimedKg);
  return {
    claimedKg,
    limit,
    isInvalid: kgText.trim() !== '' && isZeroOrUnreadable,
    isOverLimit: claimedKg !== undefined && exceedsShipmentKg(claimedKg, limit),
    isUsable: !isZeroOrUnreadable,
  };
}
