import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';
import { useConfirmPickup } from '../hooks/useConfirmPickup';

export type PickupConfirmationProps = {
  shipmentId: string;
};

/** Şoför akışı: tek büyük buton; tekrar basılsa da Idempotency-Key çift kaydı önler. */
export function PickupConfirmation({ shipmentId }: PickupConfirmationProps) {
  const { t } = useTranslation();
  const mutation = useConfirmPickup(shipmentId);
  return (
    <Stack>
      <Text size="sm">{t('field.confirmPickupHint')}</Text>
      {mutation.isError && <Alert color="red">{t('field.actionFailed')}</Alert>}
      <Button
        size="xl"
        fullWidth
        loading={mutation.isPending}
        onClick={() => {
          mutation.mutate();
        }}
      >
        {t('field.confirmPickup')}
      </Button>
    </Stack>
  );
}
