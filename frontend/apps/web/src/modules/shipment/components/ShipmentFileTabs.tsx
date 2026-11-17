import { useTranslation } from '@iplik/i18n';
import { Tabs, Text } from '@iplik/ui';
import { EvidenceGallery } from '@/modules/evidence';
import { ClaimsTabTrigger, QualityClaimsSection } from '@/modules/claim';
import { CorrectionsTab, useCanSeeCorrections } from '../corrections/CorrectionsTab';
import { SettlementTabPanels } from '../settlement/SettlementTabPanels';
import { SettlementTabTriggers } from '../settlement/SettlementTabTriggers';
import { BagsTab } from './BagsTab';
import { HistoryTab } from './HistoryTab';
import type { ShipmentDetail } from '@iplik/api-client';
import { isCompleted } from './shipmentViews';
import { WeightsPanel } from './WeightsPanel';

/** Sevkiyat dosyasının sekmeleri; düzeltmeler yalnızca tamamlanıp kilitlenen sevkiyatta sunulur, öncesinde boş kalmasın. */
export function ShipmentFileTabs({ shipment }: { shipment: ShipmentDetail }) {
  const { t } = useTranslation();
  const shipmentId = shipment.shipmentId;
  const canSeeCorrections =
    useCanSeeCorrections() && isCompleted(shipment) && shipment.audience === 'INTERNAL';
  return (
    <Tabs defaultValue="weights" keepMounted={false}>
      <Tabs.List>
        <Tabs.Tab value="weights">{t('shipment.file.tabs.weights')}</Tabs.Tab>
        <Tabs.Tab value="bags">{t('shipment.file.tabs.bags')}</Tabs.Tab>
        <Tabs.Tab value="history">{t('shipment.file.tabs.history')}</Tabs.Tab>
        {canSeeCorrections && (
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
      {canSeeCorrections && (
        <Tabs.Panel value="corrections" pt="md">
          <Text size="sm" c="dimmed" mb="sm">
            {t('shipment.file.tabHints.corrections')}
          </Text>
          <CorrectionsTab shipment={shipment} />
        </Tabs.Panel>
      )}
      <Tabs.Panel value="claims" pt="md">
        <Text size="sm" c="dimmed" mb="sm">
          {t('shipment.file.tabHints.claims')}
        </Text>
        <QualityClaimsSection shipmentId={shipmentId} />
      </Tabs.Panel>
      {isCompleted(shipment) && <SettlementTabPanels shipmentId={shipmentId} />}
      <Tabs.Panel value="evidence" pt="md">
        <EvidenceGallery shipmentId={shipmentId} />
      </Tabs.Panel>
    </Tabs>
  );
}
