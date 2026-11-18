import { useTranslation } from '@iplik/i18n';
import { Alert, Icon, Text } from '@iplik/ui';
import { formatKgText } from '@iplik/units';

export type BagWeightMessageProps = { bagKg: string; measuredKg: string; diffKg: string };

/** Çuvalların kilo toplamı araç net kilosunu tutmuyor. */
export function BagWeightMessage({ bagKg, measuredKg, diffKg }: BagWeightMessageProps) {
  const { t } = useTranslation();
  return (
    <>
      <Text>
        {t('dispatch.bagMismatch.body', {
          bagKg: formatKgText(bagKg),
          measuredKg: formatKgText(measuredKg),
        })}
      </Text>
      <Alert color="red" icon={<Icon name="discrepancies" />}>
        {t('dispatch.bagMismatch.blocked', { diff: formatKgText(diffKg) })}
      </Alert>
    </>
  );
}
