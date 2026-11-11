import { useTranslation } from '@iplik/i18n';
import { Alert, Anchor, PageHero, Paper, Stack } from '@iplik/ui';
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
            <PageHero title={t('dispatch.title')} subtitle={shipment.shipmentNo} />
            {shipment.allowedActions.includes('RECORD_DISPATCH') &&
            shipment.status !== 'WEIGHT_MISMATCH' ? (
              <Paper withBorder p="lg" radius="md" maw={560}>
                <DispatchForm
                  shipmentId={shipment.shipmentId}
                  onSaved={() => {
                    void navigate(back);
                  }}
                />
              </Paper>
            ) : (
              <Alert color="yellow">{t('dispatch.notDraft')}</Alert>
            )}
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
