import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Modal, Stack, Text, useDisclosure } from '@iplik/ui';
import { useConfirmPickup } from '../hooks/useConfirmPickup';

export type PickupConfirmationProps = {
  shipmentId: string;
};

/**
 * Şoför akışı: tek büyük buton, geri alınamadığı için önce onay sorulur (H-18). Tekrar basılsa da
 * Idempotency-Key çift kaydı önler.
 */
export function PickupConfirmation({ shipmentId }: PickupConfirmationProps) {
  const { t } = useTranslation();
  const mutation = useConfirmPickup(shipmentId);
  const [isAsking, { open, close }] = useDisclosure(false);
  return (
    <Stack>
      <Text size="sm">{t('field.confirmPickupHint')}</Text>
      {mutation.isError && <Alert color="red">{t('field.actionFailed')}</Alert>}
      <Button size="xl" fullWidth loading={mutation.isPending} onClick={open}>
        {t('field.confirmPickup')}
      </Button>
      <Modal
        opened={isAsking}
        onClose={close}
        title={t('field.confirmPickupTitle')}
        closeButtonProps={{ 'aria-label': t('field.confirmPickupClose') }}
      >
        <Stack>
          <Text>{t('field.confirmPickupQuestion')}</Text>
          <Group grow>
            <Button variant="default" onClick={close}>
              {t('field.confirmPickupDismiss')}
            </Button>
            <Button
              onClick={() => {
                close();
                mutation.mutate();
              }}
            >
              {t('field.confirmPickupAccept')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
