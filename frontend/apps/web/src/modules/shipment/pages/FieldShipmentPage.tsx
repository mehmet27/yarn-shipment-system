import { usePermission } from '@/modules/auth';
import { QualityClaimsSection } from '@/modules/claim';
import { EvidenceUpload } from '@/modules/evidence';
import { useTranslation } from '@iplik/i18n';
import { ActionBar, Anchor, Button, Stack } from '@iplik/ui';
import { Link, useParams } from 'react-router';
import { ArrivalDeclaration } from '../components/ArrivalDeclaration';
import { PickupConfirmation } from '../components/PickupConfirmation';
import { QueryState } from '../components/QueryState';
import { FieldShipmentSummary } from '../components/FieldShipmentSummary';
import { isDelivered } from '../components/shipmentViews';
import { useShipment } from '../hooks/useShipment';

/** Şoför ekranı: yalnızca çuval sayısı ve mühür no (yetkilendirme Y1); kilo hiç gelmez. */
export function FieldShipmentPage() {
  const { t } = useTranslation();
  const { shipmentId = '' } = useParams();
  const query = useShipment(shipmentId);
  const canViewClaims = usePermission('DISCREPANCY_VIEW');
  return (
    <Stack>
      <Anchor component={Link} to="/saha">
        {t('field.back')}
      </Anchor>
      <QueryState query={query}>
        {(shipment) => (
          <Stack>
            <FieldShipmentSummary shipment={shipment} />
            {shipment.allowedActions.includes('CONFIRM_PICKUP') && (
              <PickupConfirmation shipmentId={shipment.shipmentId} />
            )}
            {shipment.allowedActions.includes('DECLARE_ARRIVAL') && (
              <ArrivalDeclaration shipmentId={shipment.shipmentId} />
            )}
            <EvidenceUpload shipmentId={shipment.shipmentId} />
            {shipment.allowedActions.includes('RECORD_DELIVERY') && (
              <ActionBar>
                <Button size="xl" fullWidth component={Link} to="teslim" relative="path">
                  {t('field.recordDelivery')}
                </Button>
              </ActionBar>
            )}
            {canViewClaims && isDelivered(shipment) && (
              <QualityClaimsSection shipmentId={shipment.shipmentId} />
            )}
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
