import { DiscrepancyStatus, DISCREPANCIES_PATH, listDiscrepancies } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { ActionIcon, Indicator, Menu, NavIcon, Text } from '@iplik/ui';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { usePermission } from '@/modules/auth';
import { DiscrepancyMenuItems } from './DiscrepancyMenuItems';
import { WeightDecisionItems } from './WeightDecisionItems';
import { usePendingWeightDecisions } from '@/modules/shipment';

const POLL_MS = 60_000;
const PREVIEW_SIZE = 5;
const MENU_WIDTH_PX = 320;

/** Üst çubuktaki zil: kantar kararı bekleyen sevkiyatları ve karar bekleyen (açık) uyuşmazlıkları gösterir. */
export function NotificationBell() {
  const { t } = useTranslation();
  const canView = usePermission('DISCREPANCY_VIEW');
  const { data } = useQuery({
    queryKey: [DISCREPANCIES_PATH, 'bell'],
    queryFn: () => listDiscrepancies({ status: DiscrepancyStatus.OPEN, size: PREVIEW_SIZE }),
    enabled: canView,
    refetchInterval: POLL_MS,
  });
  const weightPending = usePendingWeightDecisions(POLL_MS);
  if (!canView) {
    return null;
  }
  const discrepancyTotal = data?.page.totalElements ?? 0;
  const items = data?.items ?? [];
  const total = discrepancyTotal + weightPending.length;
  return (
    <Menu position="bottom-end" width={MENU_WIDTH_PX} shadow="lg" radius="md">
      <Menu.Target>
        <ActionIcon variant="subtle" color="white" size={44} aria-label={t('notifications.label')}>
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
        <Menu.Item component={Link} to="/panel/uyusmazliklar?durum=OPEN">
          {t('notifications.all')}
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}
