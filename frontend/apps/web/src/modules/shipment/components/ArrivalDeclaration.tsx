import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';
import { useDeclareArrival } from '../hooks/useDeclareArrival';

export type ArrivalDeclarationProps = {
  shipmentId: string;
};

/** Şoför akışı: mühür durumuna göre iki büyük buton; seçim tek dokunuşla beyanı gönderir. */
export function ArrivalDeclaration({ shipmentId }: ArrivalDeclarationProps) {
  const { t } = useTranslation();
  const mutation = useDeclareArrival(shipmentId);
  return (
    <Stack>
      <Text size="sm">{t('field.declareArrivalHint')}</Text>
      {mutation.isError && <Alert color="red">{t('field.actionFailed')}</Alert>}
      <Button
        size="xl"
        fullWidth
        disabled={mutation.isPending}
        loading={mutation.isPending && mutation.variables}
        onClick={() => {
          mutation.mutate(true);
        }}
      >
        {t('field.arrivedSealIntact')}
      </Button>
      <Button
        size="xl"
        fullWidth
        color="red"
        variant="outline"
        disabled={mutation.isPending}
        loading={mutation.isPending && !mutation.variables}
        onClick={() => {
          mutation.mutate(false);
        }}
      >
        {t('field.arrivedSealBroken')}
      </Button>
    </Stack>
  );
}
