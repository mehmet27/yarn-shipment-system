import { useTranslation } from '@iplik/i18n';
import { Alert, Anchor, Stack, Text, Title } from '@iplik/ui';
import { Link, useNavigate, useParams } from 'react-router';
import { QueryState } from '../components/QueryState';
import { DeliveryWizard } from '../delivery/DeliveryWizard';
import { useShipment } from '../hooks/useShipment';

/** Fasoncu teslim ekranı; görünümde çıkış tartısı ve beklenen yük zaten yoktur (SEC-05). */
export function DeliveryReceiptPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { shipmentId = '' } = useParams();
  const query = useShipment(shipmentId);
  const back = `/saha/sevkiyat/${shipmentId}`;
  return (
    <Stack>
      <Anchor component={Link} to={back}>
        {t('field.back')}
      </Anchor>
      <QueryState query={query}>
        {(shipment) => (
          <Stack>
            <Title order={3}>{`${t('delivery.title')} · ${shipment.shipmentNo}`}</Title>
            <Text size="sm" c="dimmed">
              {t('delivery.blindHint')}
            </Text>
            {shipment.allowedActions.includes('RECORD_DELIVERY') ? (
              <DeliveryWizard
                shipmentId={shipment.shipmentId}
                onSubmitted={() => {
                  void navigate(back, { state: { deliverySubmitted: true } });
                }}
              />
            ) : (
              <Alert color="yellow">{t('delivery.invalidStatus')}</Alert>
            )}
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
