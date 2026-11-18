import { useTranslation } from '@iplik/i18n';
import { AppShell, Box, Burger, Group, Stack, Text, useDisclosure } from '@iplik/ui';
import { BrandMark } from '@/modules/branding';
import { Outlet } from 'react-router';
import { NotificationBell } from './NotificationBell';
import { PanelNav } from './PanelNav';
import { UserMenu } from './UserMenu';

const HEADER_HEIGHT_PX = 56;
const NAVBAR_WIDTH_PX = 248;
const NAV_BRAND_HEIGHT_PX = 44;
const HEADER_STYLE = {
  background: 'var(--app-surface)',
  borderBottom: '1px solid var(--app-line)',
} as const;
const NAVBAR_STYLE = {
  background: 'var(--app-frame-bg)',
  borderRight: '1px solid var(--app-line)',
} as const;

/**
 * Panel kabuğu: solda boydan boya açık yan menü (üstünde firma adı), sağda ince üst çubuk ve içerik. Telefonda yan
 * menü kapalıdır; firma adı üst çubukta görünür.
 */
export function PanelLayout() {
  const { t } = useTranslation();
  const [isMobileNavOpen, { toggle: toggleMobileNav, close: closeMobileNav }] = useDisclosure();
  const [isDesktopNavOpen, { toggle: toggleDesktopNav }] = useDisclosure(true);
  return (
    <AppShell
      layout="alt"
      header={{ height: HEADER_HEIGHT_PX }}
      navbar={{
        width: NAVBAR_WIDTH_PX,
        breakpoint: 'sm',
        collapsed: { mobile: !isMobileNavOpen, desktop: !isDesktopNavOpen },
      }}
      padding={{ base: 'md', sm: 'lg' }}
    >
      <AppShell.Header style={HEADER_STYLE}>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group wrap="nowrap" miw={0}>
            <Burger
              opened={isMobileNavOpen}
              onClick={toggleMobileNav}
              hiddenFrom="sm"
              aria-label={t('nav.toggle')}
            />
            <Burger
              opened={isDesktopNavOpen}
              onClick={toggleDesktopNav}
              visibleFrom="sm"
              aria-label={t('nav.toggle')}
            />
            <Box hiddenFrom="sm" miw={0}>
              <BrandMark />
            </Box>
          </Group>
          <Group gap="xs" wrap="nowrap">
            <NotificationBell />
            <UserMenu />
          </Group>
        </Group>
      </AppShell.Header>
      <AppShell.Navbar p="sm" style={NAVBAR_STYLE}>
        <Group h={NAV_BRAND_HEIGHT_PX} px="xs" justify="space-between" wrap="nowrap">
          <BrandMark />
          <Burger opened onClick={toggleMobileNav} hiddenFrom="sm" aria-label={t('nav.toggle')} />
        </Group>
        <Stack gap={2} mt="xs">
          <Text size="xs" fw={500} c="dimmed" px="sm" py="xs">
            {t('nav.panel')}
          </Text>
          <PanelNav onNavigate={closeMobileNav} />
        </Stack>
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
