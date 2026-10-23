const KNOWN_CODES = [
  'BAG_NOT_FOUND',
  'BAG_NOT_AVAILABLE',
  'BAG_ALREADY_RESERVED',
  'INVALID_SHIPMENT_STATUS',
  'BAG_SERVICE_UNAVAILABLE',
] as const;

/** Sözleşmedeki çuval atama hata kodları; bilinmeyen kod genel mesaja düşer (`bags.assign.errors.*`). */
export function assignErrorKey(code: string): string {
  const known = KNOWN_CODES.find((candidate) => candidate === code);
  return `shipment.bags.assign.errors.${known ?? 'unknown'}`;
}
