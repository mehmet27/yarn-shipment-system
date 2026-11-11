import { Tabs } from '@iplik/ui';
import { FabricReceiptsTab } from './FabricReceiptsTab';
import { FireFileTab } from './FireFileTab';
import { YarnReturnsTab } from './YarnReturnsTab';

export function SettlementTabPanels({ shipmentId }: { shipmentId: string }) {
  return (
    <>
      <Tabs.Panel value="fabric" pt="md">
        <FabricReceiptsTab shipmentId={shipmentId} />
      </Tabs.Panel>
      <Tabs.Panel value="yarn" pt="md">
        <YarnReturnsTab shipmentId={shipmentId} />
      </Tabs.Panel>
      <Tabs.Panel value="fire" pt="md">
        <FireFileTab shipmentId={shipmentId} />
      </Tabs.Panel>
    </>
  );
}
