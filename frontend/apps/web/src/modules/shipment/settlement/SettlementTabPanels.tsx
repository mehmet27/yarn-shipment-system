import { useTranslation } from '@iplik/i18n';
import { Tabs, Text } from '@iplik/ui';
import { FabricReceiptsTab } from './FabricReceiptsTab';
import { FireFileTab } from './FireFileTab';
import { YarnReturnsTab } from './YarnReturnsTab';

export function SettlementTabPanels({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  return (
    <>
      <Tabs.Panel value="fabric" pt="md">
        <Text size="sm" c="dimmed" mb="sm">
          {t('shipment.file.tabHints.fabric')}
        </Text>
        <FabricReceiptsTab shipmentId={shipmentId} />
      </Tabs.Panel>
      <Tabs.Panel value="yarn" pt="md">
        <YarnReturnsTab shipmentId={shipmentId} />
      </Tabs.Panel>
      <Tabs.Panel value="fire" pt="md">
        <Text size="sm" c="dimmed" mb="sm">
          {t('shipment.file.tabHints.fire')}
        </Text>
        <FireFileTab shipmentId={shipmentId} />
      </Tabs.Panel>
    </>
  );
}
