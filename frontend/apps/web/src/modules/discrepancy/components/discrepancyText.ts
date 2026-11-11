import { formatKg, isKilogram } from '@iplik/units';
import type { DiscrepancyView } from '@iplik/api-client';
import { formatDifference } from './formatDifference';

/** Türkçe etiketi olan kural kodları (`discrepancy.rule.*`); bilinmeyen kod ham gösterilir. */
export const KNOWN_RULES: readonly string[] = ['QUALITY_CLAIM'];

type Translate = (key: string) => string;

export function ruleLabel(t: Translate, ruleCode: string): string {
  return KNOWN_RULES.includes(ruleCode) ? t(`discrepancy.rule.${ruleCode}`) : ruleCode;
}

/** Fiş değerlerinde "gross=… tare=… net=…" biçimi (backend ScaleWeights) okunur yazılır. */
const WEIGHING_PATTERN = /^gross=(\S+) tare=(\S+) net=(\S+)$/;
const WEIGHING_PARTS = ['shipment.file.gross', 'shipment.file.tare', 'shipment.file.net'];

function kgText(value: string): string {
  return isKilogram(value) ? formatKg(value) : value;
}

/** Kuralın ürettiği beklenen/gerçek değeri okunur yazar: kilo "1.000,5 kg", tartı "Brüt … · Dara …". */
export function formatFactValue(t: Translate, value: string | undefined): string | undefined {
  if (value === undefined) return undefined;
  const weighing = WEIGHING_PATTERN.exec(value);
  if (weighing !== null) {
    return WEIGHING_PARTS.map(
      (key, index) => `${t(key)} ${kgText(weighing[index + 1] ?? '')}`,
    ).join(' · ');
  }
  return value.includes('.') ? kgText(value) : value;
}

/** Beklenen/gerçek değer özeti; ikisi de yoksa boş. */
function factSummary(t: Translate, expected?: string, actual?: string): string | undefined {
  if (expected === undefined || actual === undefined) return undefined;
  return `${t('discrepancy.facts.expected')} ${expected}, ${t('discrepancy.facts.actual').toLocaleLowerCase('tr')} ${actual}`;
}

/** Liste kartındaki özet: fark varsa kg ve yüzde, yoksa kural ve beklenen/gerçek değer (H-23). */
export function summaryOf(t: Translate, d: DiscrepancyView): string {
  const difference = formatDifference(d.diffKg, d.diffPct);
  if (difference !== '') return difference;
  const expected = formatFactValue(t, d.expectedValue);
  const actual = formatFactValue(t, d.actualValue);
  const values = factSummary(t, expected, actual);
  return [ruleLabel(t, d.ruleCode), values].filter((part) => part !== undefined).join(' · ');
}
