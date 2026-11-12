import { useTranslation } from '@iplik/i18n';
import { Stack, Tabs } from '@iplik/ui';
import { EvidenceGallery } from '@/modules/evidence';
import { ClaimsTabTrigger, QualityClaimsSection } from '@/modules/claim';
import { useParams } from 'react-router';
import { QueryState } from '../components/QueryState';
import { BagsTab } from '../components/BagsTab';
import { CorrectionsTab, useCanSeeCorrections } from '../corrections/CorrectionsTab';
import { HistoryTab } from '../components/HistoryTab';
import { ShipmentFileTop } from '../components/ShipmentFileTop';
import { isCompleted } from '../components/shipmentViews';
import { WeightsPanel } from '../components/WeightsPanel';
import { useShipment } from '../hooks/useShipment';
import { SettlementTabPanels } from '../settlement/SettlementTabPanels';
import { SettlementTabTriggers } from '../settlement/SettlementTabTriggers';

/** Mimari doküman "Sevkiyat dosyası ekranı": Yönetici ve Kalite için merkez ekran. */
export function ShipmentFilePage() {
  const { t } = useTranslation();
  const { shipmentId = '' } = useParams();
  const query = useShipment(shipmentId);
  const canSeeCorrections = useCanSeeCorrections();
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
              {isCompleted(shipment) && <ClaimsTabTrigger />}
              {isCompleted(shipment) && <SettlementTabTriggers />}
              <Tabs.Tab value="evidence">{t('shipment.file.tabs.evidence')}</Tabs.Tab>
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
            {isCompleted(shipment) && <SettlementTabPanels shipmentId={shipmentId} />}
            <Tabs.Panel value="evidence" pt="md">
              <EvidenceGallery shipmentId={shipmentId} />
            </Tabs.Panel>
          </Tabs>
        </Stack>
      )}
    </QueryState>
  );
}
