import { useTranslation } from '@iplik/i18n';
import { Alert, Center, Loader, Stack, Text, Title } from '@iplik/ui';
import { claimErrorKey, isNotAvailable } from '../claimErrors';
import { useShipmentClaims } from '../hooks/useClaims';
import { ShipmentClaims } from './ShipmentClaims';

/** Sevkiyatın kalite itirazları; itiraz teslim karşılaştırmasından sonra açılabilir. */
export function QualityClaimsSection({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const query = useShipmentClaims(shipmentId);
  return (
    <Stack>
      <Title order={2} size="h3">
        {t('claim.title')}
      </Title>
      {query.isPending && (
        <Center py="md">
          <Loader />
        </Center>
      )}
      {query.isError &&
        (isNotAvailable(query.error) ? (
          <Text c="dimmed">{t('claim.notAvailable')}</Text>
        ) : (
          <Alert color="red">{t(claimErrorKey(query.error))}</Alert>
        ))}
      {query.isSuccess && <ShipmentClaims shipmentId={shipmentId} view={query.data} />}
    </Stack>
  );
}
