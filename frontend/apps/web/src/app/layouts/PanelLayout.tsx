import { Can } from '@/modules/auth';
import { useTranslation } from '@iplik/i18n';
import { AppShell, Burger, Group, NavLink, Text, useDisclosure } from '@iplik/ui';
import { NavLink as RouterNavLink, Outlet, useLocation } from 'react-router';
import { LogoutButton } from './LogoutButton';

const HEADER_HEIGHT_PX = 56;
const NAVBAR_WIDTH_PX = 240;

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
        <NavLink
          component={RouterNavLink}
          to="/panel/sevkiyat"
          label={t('nav.shipments')}
          active={pathname.startsWith('/panel/sevkiyat')}
        />
        <Can permission="BAG_CREATE">
          <NavLink
            component={RouterNavLink}
            to="/panel/cuvallar"
            label={t('nav.bags')}
            active={pathname.startsWith('/panel/cuvallar')}
          />
        </Can>
        <Can permission="DISCREPANCY_VIEW">
          <NavLink
            component={RouterNavLink}
            to="/panel/uyusmazliklar"
            label={t('nav.discrepancies')}
            active={pathname.startsWith('/panel/uyusmazliklar')}
          />
        </Can>
        <Can permission="MASTER_DATA_MANAGE">
          <NavLink
            component={RouterNavLink}
            to="/panel/tanimlar"
            label={t('nav.masterData')}
            active={pathname.startsWith('/panel/tanimlar')}
          />
        </Can>
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
