import { useTranslation } from '@iplik/i18n';
import { Tabs } from '@iplik/ui';
import { useSettlementAccess } from './useSettlementAccess';

/** Tamamlanmış sevkiyatta izne göre Kumaş girişi, İplik dönüşü ve Fire dosyası sekmeleri. */
export function SettlementTabTriggers() {
  const { t } = useTranslation();
  const access = useSettlementAccess();
  return (
    <>
      {access.fabric && <Tabs.Tab value="fabric">{t('shipment.file.tabs.fabric')}</Tabs.Tab>}
      {access.yarn && <Tabs.Tab value="yarn">{t('shipment.file.tabs.yarn')}</Tabs.Tab>}
      {access.fire && <Tabs.Tab value="fire">{t('shipment.file.tabs.fire')}</Tabs.Tab>}
    </>
  );
}
