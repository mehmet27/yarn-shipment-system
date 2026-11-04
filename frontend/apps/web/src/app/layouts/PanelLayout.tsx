import { useTranslation } from '@iplik/i18n';
import { AppShell, Burger, Group, Text, useDisclosure } from '@iplik/ui';
import { Outlet } from 'react-router';
import { ChangePasswordButton } from './ChangePasswordButton';
import { LogoutButton } from './LogoutButton';
import { PanelNav } from './PanelNav';

const HEADER_HEIGHT_PX = 56;
const NAVBAR_WIDTH_PX = 240;

/** Masaüstü kabuğu: yan menü, üst bar, geniş içerik. */
export function PanelLayout() {
  const { t } = useTranslation();
  const [isNavOpen, { toggle }] = useDisclosure();
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
          <Group gap={0}>
            <ChangePasswordButton />
            <LogoutButton />
          </Group>
        </Group>
      </AppShell.Header>
      <AppShell.Navbar p="xs">
        <PanelNav />
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
