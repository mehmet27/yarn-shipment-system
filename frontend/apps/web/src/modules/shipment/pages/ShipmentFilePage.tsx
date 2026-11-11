import { useTranslation } from '@iplik/i18n';
import { Stack, Tabs, Text } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { QualityClaimsSection } from '@/modules/claim';
import { useParams } from 'react-router';
import { QueryState } from '../components/QueryState';
import { BagsTab } from '../components/BagsTab';
import { CorrectionsTab, useCanSeeCorrections } from '../corrections/CorrectionsTab';
import { HistoryTab } from '../components/HistoryTab';
import { ShipmentFileTop } from '../components/ShipmentFileTop';
import { isCompleted } from '../components/shipmentViews';
import { WeightsPanel } from '../components/WeightsPanel';
import { useShipment } from '../hooks/useShipment';

const PENDING_TABS = ['evidence'] as const;

/** Mimari doküman "Sevkiyat dosyası ekranı": Yönetici ve Kalite için merkez ekran. */
export function ShipmentFilePage() {
  const { t } = useTranslation();
  const { shipmentId = '' } = useParams();
  const query = useShipment(shipmentId);
  const canSeeCorrections = useCanSeeCorrections();
  const canViewClaims = usePermission('DISCREPANCY_VIEW');
  return (
    <QueryState query={query}>
      {(shipment) => (
        <Stack>
          <ShipmentFileTop shipment={shipment} />
          <Tabs defaultValue="weights" keepMounted={false}>
            <Tabs.List>
              <Tabs.Tab value="weights">{t('shipment.file.tabs.weights')}</Tabs.Tab>
              <Tabs.Tab value="bags">{t('shipment.file.tabs.bags')}</Tabs.Tab>
              <Tabs.Tab value="history">{t('shipment.file.tabs.history')}</Tabs.Tab>
              {canSeeCorrections && shipment.audience === 'INTERNAL' && (
                <Tabs.Tab value="corrections">{t('shipment.file.tabs.corrections')}</Tabs.Tab>
              )}
              {canViewClaims && isCompleted(shipment) && (
                <Tabs.Tab value="claims">{t('shipment.file.tabs.claims')}</Tabs.Tab>
              )}
              {PENDING_TABS.map((tab) => (
                <Tabs.Tab key={tab} value={tab} children={t(`shipment.file.tabs.${tab}`)} />
              ))}
            </Tabs.List>
            <Tabs.Panel value="weights" pt="md">
              <WeightsPanel shipment={shipment} />
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
            {canSeeCorrections && shipment.audience === 'INTERNAL' && (
              <Tabs.Panel value="corrections" pt="md">
                <CorrectionsTab shipment={shipment} />
              </Tabs.Panel>
            )}
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
