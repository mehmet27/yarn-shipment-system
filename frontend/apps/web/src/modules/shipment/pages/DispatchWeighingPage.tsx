import { useTranslation } from '@iplik/i18n';
import { Alert, Anchor, Stack, Text, Title } from '@iplik/ui';
import { Link, useNavigate, useParams } from 'react-router';
import { QueryState } from '../components/QueryState';
import { DispatchForm } from '../dispatch/DispatchForm';
import { useShipment } from '../hooks/useShipment';

/** Depo çıkış tartısı ekranı; yalnızca backend `RECORD_DISPATCH` aksiyonunu verdiyse form açılır. */
export function DispatchWeighingPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { shipmentId = '' } = useParams();
  const query = useShipment(shipmentId);
  const back = `/panel/sevkiyat/${shipmentId}`;
  return (
    <Stack>
      <Anchor component={Link} to={back}>
        {t('dispatch.back')}
      </Anchor>
      <QueryState query={query}>
        {(shipment) => (
          <Stack>
            <Stack gap={2}>
              <Title order={1} size="h3">
                {t('dispatch.title')}
              </Title>
              <Text c="dimmed">{shipment.shipmentNo}</Text>
            </Stack>
            {shipment.allowedActions.includes('RECORD_DISPATCH') ? (
              <DispatchForm
                shipmentId={shipment.shipmentId}
                onSaved={() => {
                  void navigate(back);
                }}
              />
            ) : (
              <Alert color="yellow">{t('dispatch.notDraft')}</Alert>
            )}
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
