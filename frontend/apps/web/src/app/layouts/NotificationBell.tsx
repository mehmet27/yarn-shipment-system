import { DiscrepancyStatus, DISCREPANCIES_PATH, listDiscrepancies } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { ActionIcon, Indicator, Menu, NavIcon, Text } from '@iplik/ui';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { usePermission } from '@/modules/auth';

const POLL_MS = 60_000;
const PREVIEW_SIZE = 5;
const MENU_WIDTH_PX = 320;

/** Üst çubuktaki zil: karar bekleyen (açık) uyuşmazlıkları sayar ve ilk birkaçını listeler. */
export function NotificationBell() {
  const { t } = useTranslation();
  const canView = usePermission('DISCREPANCY_VIEW');
  const { data } = useQuery({
    queryKey: [DISCREPANCIES_PATH, 'bell'],
    queryFn: () => listDiscrepancies({ status: DiscrepancyStatus.OPEN, size: PREVIEW_SIZE }),
    enabled: canView,
    refetchInterval: POLL_MS,
  });
  if (!canView) {
    return null;
  }
  const total = data?.page.totalElements ?? 0;
  const items = data?.items ?? [];
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
        <Menu.Label>{t('notifications.title')}</Menu.Label>
        {items.length === 0 && (
          <Text size="sm" c="dimmed" px="sm" py="xs">
            {t('notifications.empty')}
          </Text>
        )}
        {items.map((item) => (
          <Menu.Item key={item.id} component={Link} to={`/panel/uyusmazliklar/${item.id}`}>
            <Text size="sm" fw={600}>
              {item.discrepancyNo}
            </Text>
            <Text size="xs" c="dimmed">
              {t(`discrepancy.type.${item.type}`)}
            </Text>
          </Menu.Item>
        ))}
        {total > items.length && (
          <Text size="xs" c="dimmed" px="sm" py={4}>
            {t('notifications.more', { count: total - items.length })}
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
