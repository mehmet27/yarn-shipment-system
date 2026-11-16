import { useTranslation } from '@iplik/i18n';
import { Button, SimpleGrid, Stack, Text, Title } from '@iplik/ui';
import { Link } from 'react-router';
import { Can, useCurrentUser } from '@/modules/auth';
import type { Permission } from '@iplik/api-client';
import { DashboardSection } from '@/modules/report';
import { PendingDispatches, PendingWeightDecisions } from '@/modules/shipment';

/** Yönetici yalnızca sistemi yönetir; ana sayfada yetkisi olduğu sistem ekranlarının kısayolları görünür. */
const SYSTEM_SHORTCUTS: readonly { to: string; labelKey: string; permission: Permission }[] = [
  { to: '/panel/kullanicilar', labelKey: 'nav.users', permission: 'USER_MANAGE' },
  { to: '/panel/yetkiler', labelKey: 'nav.permissions', permission: 'PERMISSION_MANAGE' },
  { to: '/panel/tanimlar', labelKey: 'nav.masterData', permission: 'MASTER_DATA_MANAGE' },
  { to: '/panel/partiler', labelKey: 'nav.lots', permission: 'LOT_QUALITY_LOCK' },
  { to: '/panel/gorunum', labelKey: 'nav.branding', permission: 'SETTINGS_MANAGE' },
  { to: '/panel/denetim', labelKey: 'nav.audit', permission: 'AUDIT_VIEW' },
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
                <Button component={Link} to="/panel/sevkiyat" size="lg" variant="light">
                  {t('nav.shipments')}
                </Button>
              </Can>
              {SYSTEM_SHORTCUTS.map((item) => (
                <Can key={item.to} permission={item.permission}>
                  <Button component={Link} to={item.to} size="lg" variant="light">
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
