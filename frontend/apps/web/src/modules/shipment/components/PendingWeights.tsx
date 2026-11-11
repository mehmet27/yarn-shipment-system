import { useTranslation } from '@iplik/i18n';
import { formatKgText } from '@iplik/units';
import { CloseButton, Group, Paper, Text } from '@iplik/ui';

/** Eklenmeyi bekleyen çuvallar: sıra no (sevkiyattakilerden sonra gelir) ve kg; her biri çıkarılabilir. */
export function PendingWeights({
  weights,
  firstSeq,
  onRemove,
}: {
  weights: readonly string[];
  firstSeq: number;
  onRemove: (index: number) => void;
}) {
  const { t } = useTranslation();
  return weights.map((kg, index) => (
    <Paper key={`${String(index)}-${kg}`} withBorder px="sm" py={4}>
      <Group justify="space-between">
        <Text>
          {t('shipment.bags.seq', { seq: firstSeq + index })} · {formatKgText(kg)}
        </Text>
        <CloseButton
          aria-label={t('shipment.bags.assign.remove', { seq: firstSeq + index })}
          onClick={() => {
            onRemove(index);
          }}
        />
      </Group>
    </Paper>
  ));
}
