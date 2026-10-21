import { useTranslation } from '@iplik/i18n';
import { Stack, Tabs, Text } from '@iplik/ui';
import { useParams } from 'react-router';
import { QueryState } from '../components/QueryState';
import { ShipmentHeader } from '../components/ShipmentHeader';
import { toWeighings } from '../components/shipmentViews';
import { WeightComparison } from '../components/WeightComparison';
import { useShipment } from '../hooks/useShipment';

const PENDING_TABS = ['bags', 'evidence', 'history'] as const;

/** Mimari doküman "Sevkiyat dosyası ekranı": Yönetici ve Kalite için merkez ekran. */
export function ShipmentFilePage() {
  const { t } = useTranslation();
  const { shipmentId = '' } = useParams();
  const query = useShipment(shipmentId);
  return (
    <QueryState query={query}>
      {(shipment) => (
        <Stack>
          <ShipmentHeader shipment={shipment} />
          <Tabs defaultValue="weights">
            <Tabs.List>
              <Tabs.Tab value="weights">{t('shipment.file.tabs.weights')}</Tabs.Tab>
              {PENDING_TABS.map((tab) => (
                <Tabs.Tab key={tab} value={tab}>
                  {t(`shipment.file.tabs.${tab}`)}
                </Tabs.Tab>
              ))}
            </Tabs.List>
            <Tabs.Panel value="weights" pt="md">
              <WeightComparison weighings={toWeighings(shipment)} />
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
