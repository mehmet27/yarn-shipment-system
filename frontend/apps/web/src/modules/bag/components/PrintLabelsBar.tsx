import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { usePrintLabels } from '../hooks/usePrintLabels';
import { bagErrorKey } from './bagErrors';

export type PrintLabelsBarProps = {
  uniqueKeys: string[];
  /** Seçilenlerden en az birinin etiketi daha önce basıldı mı? */
  /** Seçilenlerden etiketi daha önce basılmış olanların sayısı; 0 ise gerekçe sorulmaz. */
  reprintCount: number;
  onPrinted: (pdf: Blob) => void;
};

/** Seçili çuvalların etiketini basar; yeniden baskıda gerekçe ister. */
export function PrintLabelsBar({ uniqueKeys, reprintCount, onPrinted }: PrintLabelsBarProps) {
  const { t } = useTranslation();
  const [reason, setReason] = useState('');
  const mutation = usePrintLabels();
  const hasReprint = reprintCount > 0;
  const isReasonMissing = hasReprint && reason.trim() === '';
  const print = () => {
    const reprintReason = hasReprint ? { reprintReason: reason.trim() } : {};
    mutation.mutate(
      { uniqueKeys, ...reprintReason },
      {
        onSuccess: (pdf) => {
          setReason('');
          onPrinted(pdf);
        },
      },
    );
  };
  return (
    <Stack gap="xs">
      {hasReprint && (
        <TextInput
          label={t('bag.reprintReason')}
          description={t('bag.reprintHint', { count: reprintCount })}
          value={reason}
          onChange={(event) => {
            setReason(event.currentTarget.value);
          }}
          required
        />
      )}
      <Group>
        <Text>{t('bag.selectedCount', { count: uniqueKeys.length })}</Text>
        <Button
          onClick={print}
          loading={mutation.isPending}
          disabled={uniqueKeys.length === 0 || isReasonMissing}
        >
          {t('bag.print')}
        </Button>
      </Group>
      {mutation.isError && (
        <Alert color="red" role="alert">
          {t(bagErrorKey(mutation.error))}
        </Alert>
      )}
    </Stack>
  );
}
