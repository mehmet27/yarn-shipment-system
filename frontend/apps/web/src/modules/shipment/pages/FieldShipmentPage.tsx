import { usePermission } from '@/modules/auth';
import { QualityClaimsSection } from '@/modules/claim';
import { EvidenceUpload } from '@/modules/evidence';
import { useTranslation } from '@iplik/i18n';
import { Anchor, Stack } from '@iplik/ui';
import { Link, useParams } from 'react-router';
import { QueryState } from '../components/QueryState';
import { FieldShipmentSummary } from '../components/FieldShipmentSummary';
import { isCompleted } from '../components/shipmentViews';
import { useShipment } from '../hooks/useShipment';

/** Saha ekranı: sevkiyatın özeti, fotoğraf yükleme ve tamamlanan sevkiyatta kalite itirazı. */
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
            <EvidenceUpload shipmentId={shipment.shipmentId} />
            {canViewClaims && isCompleted(shipment) && (
              <QualityClaimsSection shipmentId={shipment.shipmentId} />
            )}
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
