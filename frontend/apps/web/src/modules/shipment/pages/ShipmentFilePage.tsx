import { useTranslation } from '@iplik/i18n';
import { Button, Stack, Tabs, Text } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { QualityClaimsSection } from '@/modules/claim';
import { EvidenceUpload } from '@/modules/evidence';
import { Link, useParams } from 'react-router';
import { QueryState } from '../components/QueryState';
import { CancelShipmentForm } from '../components/CancelShipmentForm';
import { BagsTab } from '../components/BagsTab';
import { HistoryTab } from '../components/HistoryTab';
import { ShipmentHeader } from '../components/ShipmentHeader';
import { isDelivered, toWeighings } from '../components/shipmentViews';
import { WeightComparison } from '../components/WeightComparison';
import { useShipment } from '../hooks/useShipment';

const PENDING_TABS = ['evidence'] as const;

/** Mimari doküman "Sevkiyat dosyası ekranı": Yönetici ve Kalite için merkez ekran. */
export function ShipmentFilePage() {
  const { t } = useTranslation();
  const { shipmentId = '' } = useParams();
  const query = useShipment(shipmentId);
  const canViewClaims = usePermission('DISCREPANCY_VIEW');
  return (
    <QueryState query={query}>
      {(shipment) => (
        <Stack>
          <ShipmentHeader shipment={shipment} />
          {shipment.allowedActions.includes('RECORD_DISPATCH') && (
            <Button component={Link} to="cikis" relative="path" w="fit-content">
              {t('shipment.file.recordDispatch')}
            </Button>
          )}
          {shipment.allowedActions.includes('CANCEL') && (
            <CancelShipmentForm shipmentId={shipmentId} />
          )}
          <EvidenceUpload shipmentId={shipmentId} />
          <Tabs defaultValue="weights" keepMounted={false}>
            <Tabs.List>
              <Tabs.Tab value="weights">{t('shipment.file.tabs.weights')}</Tabs.Tab>
              <Tabs.Tab value="bags">{t('shipment.file.tabs.bags')}</Tabs.Tab>
              <Tabs.Tab value="history">{t('shipment.file.tabs.history')}</Tabs.Tab>
              {canViewClaims && isDelivered(shipment) && (
                <Tabs.Tab value="claims">{t('shipment.file.tabs.claims')}</Tabs.Tab>
              )}
              {PENDING_TABS.map((tab) => (
                <Tabs.Tab key={tab} value={tab}>
                  {t(`shipment.file.tabs.${tab}`)}
                </Tabs.Tab>
              ))}
            </Tabs.List>
            <Tabs.Panel value="weights" pt="md">
              <WeightComparison weighings={toWeighings(shipment)} />
            </Tabs.Panel>
            <Tabs.Panel value="bags" pt="md">
              <BagsTab
                shipmentId={shipmentId}
                canAssign={shipment.allowedActions.includes('ASSIGN_BAGS')}
              />
            </Tabs.Panel>
            <Tabs.Panel value="history" pt="md">
              <HistoryTab shipmentId={shipmentId} />
            </Tabs.Panel>
            <Tabs.Panel value="claims" pt="md">
              <QualityClaimsSection shipmentId={shipmentId} />
            </Tabs.Panel>
            {PENDING_TABS.map((tab) => (
              <Tabs.Panel key={tab} value={tab} pt="md">
                <Text c="dimmed">{t('shipment.file.comingSoon')}</Text>
              </Tabs.Panel>
            ))}
          </Tabs>
        </Stack>
      )}
    </QueryState>
  );
}
