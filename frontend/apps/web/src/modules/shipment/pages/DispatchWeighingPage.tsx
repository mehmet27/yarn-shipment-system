import { useTranslation } from '@iplik/i18n';
import { Anchor, PageHero, Stack } from '@iplik/ui';
import { Link, useNavigate, useParams } from 'react-router';
import { QueryState } from '@/modules/shared';
import { DispatchPanel } from '../dispatch/DispatchPanel';
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
            <DispatchPanel
              shipment={shipment}
              onSaved={() => {
                void navigate(back);
              }}
            />
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
