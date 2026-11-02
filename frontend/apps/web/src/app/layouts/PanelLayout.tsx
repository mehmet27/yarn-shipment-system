import type { Permission } from '@iplik/api-client';
import { Can } from '@/modules/auth';
import { useTranslation } from '@iplik/i18n';
import { AppShell, Burger, Group, NavLink, Text, useDisclosure } from '@iplik/ui';
import { NavLink as RouterNavLink, Outlet, useLocation } from 'react-router';
import { LogoutButton } from './LogoutButton';

const HEADER_HEIGHT_PX = 56;
const NAVBAR_WIDTH_PX = 240;

/** Yan menü; izni olan bağlantı yalnızca o izinle görünür (FE-SEC-02). */
const NAV_ITEMS: readonly { to: string; label: string; permission?: Permission }[] = [
  { to: '/panel/sevkiyat', label: 'nav.shipments' },
  { to: '/panel/uyusmazliklar', label: 'nav.discrepancies', permission: 'DISCREPANCY_VIEW' },
  { to: '/panel/tanimlar', label: 'nav.masterData', permission: 'MASTER_DATA_MANAGE' },
  { to: '/panel/partiler', label: 'nav.lots', permission: 'LOT_QUALITY_LOCK' },
  { to: '/panel/kural-ayarlari', label: 'nav.discrepancySettings', permission: 'SETTINGS_MANAGE' },
];

/** Masaüstü kabuğu: yan menü, üst bar, geniş içerik. */
export function PanelLayout() {
  const { t } = useTranslation();
  const [isNavOpen, { toggle }] = useDisclosure();
  const { pathname } = useLocation();
  return (
    <AppShell
      header={{ height: HEADER_HEIGHT_PX }}
      navbar={{ width: NAVBAR_WIDTH_PX, breakpoint: 'sm', collapsed: { mobile: !isNavOpen } }}
      padding="md"
    >
      <AppShell.Header bg="brand.7" c="white">
        <Group h="100%" px="md" justify="space-between">
          <Group>
            <Burger opened={isNavOpen} onClick={toggle} hiddenFrom="sm" color="white" />
            <Text fw={700} size="lg">
              {t('app.name')}
            </Text>
          </Group>
          <LogoutButton />
        </Group>
      </AppShell.Header>
      <AppShell.Navbar p="xs">
        {NAV_ITEMS.map(({ to, label, permission }) => {
          const link = (
            <NavLink
              key={to}
              component={RouterNavLink}
              to={to}
              label={t(label)}
              active={pathname.startsWith(to)}
            />
          );
          return permission === undefined ? (
            link
          ) : (
            <Can key={to} permission={permission}>
              {link}
            </Can>
          );
        })}
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
