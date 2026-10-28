import type { DiscrepancyView } from '@iplik/api-client';
import { formatKg, isKilogram } from '@iplik/units';
import { formatDifference } from './formatDifference';

/** Türkçe etiketi olan kural kodları (`discrepancy.rule.*`); bilinmeyen kod ham gösterilir. */
export const KNOWN_RULES: readonly string[] = [
  'QUANTITY_TOLERANCE',
  'QUANTITY_DISPATCH_MISSING',
  'BAG_COUNT_MISMATCH',
  'BAG_KEYS_MISMATCH',
  'SEAL_NOT_INTACT',
  'SEAL_NO_MISMATCH',
  'DOCUMENT_MISSING',
  'DOCUMENT_SHIPMENT_NO_MISMATCH',
  'DOCUMENT_NET_KG_MISMATCH',
  'DOCUMENT_BAG_COUNT_MISMATCH',
  'TICKET_DISPATCH_PLATE_MISMATCH',
  'TICKET_DELIVERY_PLATE_MISMATCH',
  'TICKET_DISPATCH_REUSED',
  'TICKET_DELIVERY_REUSED',
  'TICKET_DISPATCH_WEIGHT_MISMATCH',
  'TICKET_DELIVERY_WEIGHT_MISMATCH',
  'TICKET_QUANTITY_TOLERANCE',
  'QUALITY_CLAIM',
];

type Translate = (key: string) => string;

export function ruleLabel(t: Translate, ruleCode: string): string {
  return KNOWN_RULES.includes(ruleCode) ? t(`discrepancy.rule.${ruleCode}`) : ruleCode;
}

/** Fiş kurallarında değer "gross=… tare=… net=…" biçimindedir (backend ScaleWeights). */
const WEIGHING_PATTERN = /^gross=(\S+) tare=(\S+) net=(\S+)$/;
const WEIGHING_PARTS = ['shipment.file.gross', 'shipment.file.tare', 'shipment.file.net'];

function kgText(value: string): string {
  return isKilogram(value) ? formatKg(value) : value;
}

/**
 * Kuralın ürettiği beklenen/gerçek değeri okunur yazar: kilo "1.000,5 kg", tartı "Brüt … · Dara …",
 * çuval sayısı ve plaka gibi diğer değerler olduğu gibi.
 */
export function formatFactValue(
  t: Translate,
  type: DiscrepancyView['type'],
  value: string | undefined,
): string | undefined {
  if (value === undefined) return undefined;
  const weighing = WEIGHING_PATTERN.exec(value);
  if (weighing !== null) {
    return WEIGHING_PARTS.map(
      (key, index) => `${t(key)} ${kgText(weighing[index + 1] ?? '')}`,
    ).join(' · ');
  }
  const isQuantity = type === 'QUANTITY' || value.includes('.');
  return isQuantity ? kgText(value) : value;
}

/** Liste kartındaki özet: fark varsa kg ve yüzde, yoksa kural ve beklenen/gerçek değer (H-23). */
export function summaryOf(t: Translate, d: DiscrepancyView): string {
  const difference = formatDifference(d.diffKg, d.diffPct);
  if (difference !== '') return difference;
  const expected = formatFactValue(t, d.type, d.expectedValue);
  const actual = formatFactValue(t, d.type, d.actualValue);
  const values =
    expected === undefined || actual === undefined
      ? undefined
      : `${t('discrepancy.facts.expected')} ${expected}, ${t('discrepancy.facts.actual').toLocaleLowerCase('tr')} ${actual}`;
  return [ruleLabel(t, d.ruleCode), values].filter((part) => part !== undefined).join(' · ');
}
