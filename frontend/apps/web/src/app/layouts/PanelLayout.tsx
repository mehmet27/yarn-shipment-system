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
import { BrandMark } from '@/modules/branding';
import { Outlet } from 'react-router';
import { PanelNav } from './PanelNav';
import { UserMenu } from './UserMenu';

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
            <BrandMark />
          </Group>
          <UserMenu />
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
