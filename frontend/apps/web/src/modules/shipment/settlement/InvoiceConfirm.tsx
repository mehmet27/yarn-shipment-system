import { useTranslation } from '@iplik/i18n';
import { Button, Group, Paper, Stack, Text } from '@iplik/ui';

export type InvoiceConfirmProps = {
  isOpen: boolean;
  loading: boolean;
  onSubmit: () => void;
  onConfirm: () => void;
  onCancel: () => void;
};

/** Fatura kaydı geri alınamaz ve faturalanan kilonun kumaşı sonradan girilemez: kaydetmeden önce onay istenir. */
export function InvoiceConfirm({
  isOpen,
  loading,
  onSubmit,
  onConfirm,
  onCancel,
}: InvoiceConfirmProps) {
  const { t } = useTranslation();
  if (!isOpen) {
    return (
      <Button w="fit-content" onClick={onSubmit}>
        {t('settlement.fire.invoiceSubmit')}
      </Button>
    );
  }
  return (
    <Paper withBorder p="sm" role="group" aria-label={t('settlement.fire.invoiceConfirmTitle')}>
      <Stack gap="sm">
        <Text size="sm">{t('settlement.fire.invoiceConfirmText')}</Text>
        <Group grow>
          <Button loading={loading} onClick={onConfirm}>
            {t('settlement.fire.invoiceConfirmYes')}
          </Button>
          <Button variant="default" onClick={onCancel}>
            {t('settlement.fire.invoiceConfirmNo')}
          </Button>
        </Group>
      </Stack>
    </Paper>
  );
}
