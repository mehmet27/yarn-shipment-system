import { useTranslation } from '@iplik/i18n';
import { ActionIcon, Indicator, Menu, NavIcon, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { usePermission } from '@/modules/auth';
import { DiscrepancyMenuItems } from './DiscrepancyMenuItems';
import { useBellDiscrepancies } from './useBellDiscrepancies';
import { WeightDecisionItems } from './WeightDecisionItems';
import { usePendingWeightDecisions } from '@/modules/shipment';

const POLL_MS = 60_000;
const MENU_WIDTH_PX = 320;

/** Üst çubuktaki zil: kantar kararı bekleyen sevkiyatları ve karar bekleyen (açık ve incelemedeki) uyuşmazlıkları, yani yeni kalite itirazlarını gösterir. */
export function NotificationBell() {
  const { t } = useTranslation();
  const canView = usePermission('DISCREPANCY_VIEW');
  const { total: discrepancyTotal, items } = useBellDiscrepancies(canView);
  const weightPending = usePendingWeightDecisions({ pollMs: POLL_MS, isEnabled: canView });
  if (!canView) {
    return null;
  }
  const total = discrepancyTotal + weightPending.length;
  return (
    <Menu position="bottom-end" width={MENU_WIDTH_PX} shadow="lg" radius="md">
      <Menu.Target>
        <ActionIcon variant="subtle" color="gray" size={44} aria-label={t('notifications.label')}>
          <Indicator label={total} size={16} disabled={total === 0} color="red" offset={2}>
            <NavIcon name="bell" />
          </Indicator>
        </ActionIcon>
      </Menu.Target>
      <Menu.Dropdown>
        <WeightDecisionItems shipments={weightPending} />
        {total === 0 && (
          <Text size="sm" c="dimmed" px="sm" py="xs">
            {t('notifications.empty')}
          </Text>
        )}
        {discrepancyTotal > 0 && <Menu.Label>{t('notifications.title')}</Menu.Label>}
        <DiscrepancyMenuItems items={items} />
        {discrepancyTotal > items.length && (
          <Text size="xs" c="dimmed" px="sm" py={4}>
            {t('notifications.more', { count: discrepancyTotal - items.length })}
          </Text>
        )}
        <Menu.Divider />
        <Menu.Item component={Link} to="/panel/uyusmazliklar">
          {t('notifications.all')}
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}
