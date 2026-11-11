import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';
import { useCloseFireSettlement } from './hooks';
import { settlementErrorKey } from './settlementErrors';

/** Hatalı kilosu olmayan dosyayı kapatır (INVOICE_MARK). */
export function CloseAction({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const mutation = useCloseFireSettlement(shipmentId);
  return (
    <Stack gap="xs">
      <Text size="sm" c="dimmed">
        {t('settlement.fire.closeHint')}
      </Text>
      {mutation.isError && <Alert color="red">{t(settlementErrorKey(mutation.error))}</Alert>}
      <Button
        w="fit-content"
        loading={mutation.isPending}
        onClick={() => {
          mutation.mutate();
        }}
      >
        {t('settlement.fire.close')}
      </Button>
    </Stack>
  );
}
