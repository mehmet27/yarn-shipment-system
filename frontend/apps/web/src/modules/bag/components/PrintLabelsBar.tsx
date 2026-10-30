import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { usePrintLabels } from '../hooks/usePrintLabels';
import { bagErrorKey } from './bagErrors';

export type PrintLabelsBarProps = {
  uniqueKeys: string[];
  /** Seçilenlerden en az birinin etiketi daha önce basıldı mı? */
  hasReprint: boolean;
  onPrinted: (pdf: Blob) => void;
};

/** Seçili çuvalların etiketini basar; yeniden baskıda gerekçe ister. */
export function PrintLabelsBar({ uniqueKeys, hasReprint, onPrinted }: PrintLabelsBarProps) {
  const { t } = useTranslation();
  const [reason, setReason] = useState('');
  const mutation = usePrintLabels();
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
          description={t('bag.reprintHint')}
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
      {mutation.isError && <Alert color="red">{t(bagErrorKey(mutation.error))}</Alert>}
    </Stack>
  );
}
