import { useTranslation } from '@iplik/i18n';
import type { NavIconName } from '@iplik/ui';
import { Button, Icon, SimpleGrid, Stack, Text, Title } from '@iplik/ui';
import { Link } from 'react-router';
import { Can, useCurrentUser } from '@/modules/auth';
import type { Permission } from '@iplik/api-client';
import { PendingDiscrepancies } from '@/modules/discrepancy';
import { DashboardSection } from '@/modules/report';
import { PendingDispatches, PendingWeightDecisions } from '@/modules/shipment';

/** Yönetici yalnızca sistemi yönetir; ana sayfada yetkisi olduğu sistem ekranlarının kısayolları görünür. */
const SYSTEM_SHORTCUTS: readonly {
  to: string;
  labelKey: string;
  permission: Permission;
  icon: NavIconName;
}[] = [
  { to: '/panel/kullanicilar', labelKey: 'nav.users', icon: 'users', permission: 'USER_MANAGE' },
  {
    to: '/panel/yetkiler',
    labelKey: 'nav.permissions',
    icon: 'permissions',
    permission: 'PERMISSION_MANAGE',
  },
  {
    to: '/panel/tanimlar',
    labelKey: 'nav.masterData',
    icon: 'masterData',
    permission: 'MASTER_DATA_MANAGE',
  },
  { to: '/panel/partiler', labelKey: 'nav.lots', icon: 'lots', permission: 'LOT_QUALITY_LOCK' },
  {
    to: '/panel/gorunum',
    labelKey: 'nav.branding',
    icon: 'branding',
    permission: 'SETTINGS_MANAGE',
  },
  { to: '/panel/denetim', labelKey: 'nav.audit', icon: 'audit', permission: 'AUDIT_VIEW' },
];

export function PanelHomePage() {
  const { t } = useTranslation();
  const { data: user } = useCurrentUser();
  return (
    <Stack>
      <Title order={1} size="h2">
        {t('home.welcome', { name: user?.fullName ?? '' })}
      </Title>
      <Can permission="SHIPMENT_VIEW">
        <PendingWeightDecisions />
      </Can>
      <Can permission="DISCREPANCY_RESOLVE">
        <PendingDiscrepancies />
      </Can>
      <Can permission="DISPATCH_RECORD">
        <PendingDispatches />
      </Can>
      <Can
        permission="REPORT_VIEW"
        fallback={
          <Stack>
            <Text c="dimmed">{t('home.shortcuts')}</Text>
            <SimpleGrid cols={{ base: 1, sm: 2 }}>
              <Can permission="SHIPMENT_VIEW">
                <Button
                  component={Link}
                  to="/panel/sevkiyat"
                  variant="light"
                  leftSection={<Icon name="shipments" />}
                >
                  {t('nav.shipments')}
                </Button>
              </Can>
              {SYSTEM_SHORTCUTS.map((item) => (
                <Can key={item.to} permission={item.permission}>
                  <Button
                    component={Link}
                    to={item.to}
                    variant="light"
                    leftSection={<Icon name={item.icon} />}
                  >
                    {t(item.labelKey)}
                  </Button>
                </Can>
              ))}
            </SimpleGrid>
          </Stack>
        }
      >
        <DashboardSection />
      </Can>
    </Stack>
  );
}
