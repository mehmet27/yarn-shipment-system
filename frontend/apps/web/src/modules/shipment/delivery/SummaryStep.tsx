import { useTranslation } from '@iplik/i18n';
import { Paper, SimpleGrid, Text } from '@iplik/ui';
import { formatKg } from '@iplik/units';
import type { DeliveryDraft } from './deliveryDraft';
import { parseWeighing } from './deliveryDraft';

export type SummaryStepProps = { draft: DeliveryDraft };

/** Yalnızca fasoncunun girdiği değerler; beklenen değerlerle karşılaştırma yapılmaz (kör giriş). */
export function SummaryStep({ draft }: SummaryStepProps) {
  const { t } = useTranslation();
  const weighing = parseWeighing(draft);
  const rows: readonly (readonly [string, string])[] = [
    [
      t('delivery.seal.condition'),
      draft.sealCondition ? t(`delivery.seal.${draft.sealCondition}`) : '',
    ],
    [t('delivery.seal.number'), draft.sealNo || t('common.notRecorded')],
    [t('delivery.summary.scannedBags'), String(draft.bagKeys.length)],
    [t('delivery.weighing.bagCount'), draft.bagCount],
    ...(typeof weighing === 'string'
      ? []
      : ([
          [t('delivery.weighing.gross'), formatKg(weighing.grossKg)],
          [t('delivery.weighing.tare'), formatKg(weighing.tareKg)],
          [t('delivery.summary.net'), formatKg(weighing.netKg)],
        ] as const)),
  ];
  return (
    <Paper withBorder p="md">
      <SimpleGrid cols={2}>
        {rows.map(([label, value]) => [
          <Text key={`${label}-label`} c="dimmed">
            {label}
          </Text>,
          <Text key={`${label}-value`} fw={700}>
            {value}
          </Text>,
        ])}
      </SimpleGrid>
    </Paper>
  );
}
