import { usePermission } from '@/modules/auth';
import { useTranslation } from '@iplik/i18n';
import { Tabs } from '@iplik/ui';

/** Sevkiyat dosyasındaki "Kalite itirazı" sekmesi; yalnızca itirazları görme izni olana görünür. */
export function ClaimsTabTrigger() {
  const { t } = useTranslation();
  const canView = usePermission('DISCREPANCY_VIEW');
  return canView ? <Tabs.Tab value="claims">{t('shipment.file.tabs.claims')}</Tabs.Tab> : null;
}
