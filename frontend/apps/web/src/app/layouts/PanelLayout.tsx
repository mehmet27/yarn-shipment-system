import { useTranslation } from '@iplik/i18n';
import {
  AppShell,
  BRAND_GRADIENT,
  BRAND_SIDEBAR_GRADIENT,
  Burger,
  Group,
  Stack,
  Text,
  useDisclosure,
} from '@iplik/ui';
import { Outlet } from 'react-router';
import { ChangePasswordButton } from './ChangePasswordButton';
import { LogoutButton } from './LogoutButton';
import { PanelNav } from './PanelNav';

const HEADER_HEIGHT_PX = 56;
const NAVBAR_WIDTH_PX = 248;

/** Masaüstü kabuğu: koyu marka yan menüsü, gradient üst bar, geniş içerik. */
export function PanelLayout() {
  const { t } = useTranslation();
  const [isNavOpen, { toggle }] = useDisclosure();
  return (
    <AppShell
      header={{ height: HEADER_HEIGHT_PX }}
      navbar={{ width: NAVBAR_WIDTH_PX, breakpoint: 'sm', collapsed: { mobile: !isNavOpen } }}
      padding="md"
    >
      <AppShell.Header c="white" style={{ background: BRAND_GRADIENT }}>
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
      <AppShell.Navbar p="sm" c="white" style={{ background: BRAND_SIDEBAR_GRADIENT, border: 0 }}>
        <Stack gap={4}>
          <Text size="xs" fw={700} tt="uppercase" lts="0.08em" c="brand.2" px="sm" py="xs">
            {t('nav.panel')}
          </Text>
          <PanelNav />
        </Stack>
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
