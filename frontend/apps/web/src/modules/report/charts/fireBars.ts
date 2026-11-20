import type { YarnVsFabricRowView } from '@iplik/api-client';
import type { BarListItem } from '@iplik/ui';
import { formatFireRate, formatKg3 } from '../format';

type Translate = (key: string, options?: Record<string, unknown>) => string;

/**
 * Fasoncu başına gerçekleşen fire oranı, en yüksek önce. Fire sınırını aşan sevkiyatı olan fasoncu dikkat rengiyle
 * çizilir ve bu, satırın altında yazıyla da söylenir (renk tek başına bilgi taşımaz).
 */
export function toFireBars(rows: readonly YarnVsFabricRowView[], t: Translate): BarListItem[] {
  return [...rows]
    .sort((a, b) => Number(b.actualFireRate) - Number(a.actualFireRate))
    .map((row) => {
      const isOverLimit = row.overLimitShipmentCount > 0;
      const amounts = t('report.charts.fire.amounts', {
        sent: formatKg3(row.sentKg),
        fabric: formatKg3(row.fabricKg),
      });
      const overLimit = t('report.charts.fire.overLimit', {
        count: row.overLimitShipmentCount,
        kg: formatKg3(row.shortageKg),
      });
      return {
        key: row.subcontractorId,
        label: row.subcontractorName,
        value: Number(row.actualFireRate),
        valueLabel: formatFireRate(row.actualFireRate),
        note: isOverLimit ? `${amounts} · ${overLimit}` : amounts,
        tone: isOverLimit ? 'danger' : 'neutral',
      };
    });
}
