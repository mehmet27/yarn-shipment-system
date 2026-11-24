import { useTranslation } from '@iplik/i18n';
import { AppShell, Box, Group, Stack, Text, useDisclosure } from '@iplik/ui';
import { HelpAssistant } from '@/modules/assistant';
import { BrandMark } from '@/modules/branding';
import { QuickSearch } from '@/modules/shipment';
import { Outlet } from 'react-router';
import { NavBackdrop } from './NavBackdrop';
import { PanelHeader } from './PanelHeader';
import { PanelNav } from './PanelNav';

/** Bu genişliğin altı telefon düzenidir. */
const MOBILE_ONLY = 'sm';
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
/** Sabit Yardım düğmesi sayfanın son satırlarını örtmesin diye alt boşluk. */
const HELP_CLEARANCE_PX = 88;

export function PanelLayout() {
  const { t } = useTranslation();
  const [isMobileNavOpen, { toggle: toggleMobileNav, close: closeMobileNav }] = useDisclosure();
  const [isDesktopNavOpen, { toggle: toggleDesktopNav }] = useDisclosure(true);
  return (
    <>
      <AppShell
        layout="alt"
        header={{ height: HEADER_HEIGHT_PX }}
        navbar={{
          width: NAVBAR_WIDTH_PX,
          breakpoint: MOBILE_ONLY,
          collapsed: { mobile: !isMobileNavOpen, desktop: !isDesktopNavOpen },
        }}
        padding={{ base: 'md', sm: 'lg' }}
      >
        <AppShell.Header style={HEADER_STYLE}>
          <PanelHeader
            isMobileNavOpen={isMobileNavOpen}
            isDesktopNavOpen={isDesktopNavOpen}
            onToggleMobileNav={toggleMobileNav}
            onToggleDesktopNav={toggleDesktopNav}
          />
        </AppShell.Header>
        <AppShell.Navbar p="sm" style={NAVBAR_STYLE} className="panel-navbar">
          <Group h={NAV_BRAND_HEIGHT_PX} px="xs" wrap="nowrap" visibleFrom={MOBILE_ONLY}>
            <Box visibleFrom={MOBILE_ONLY} miw={0}>
              <BrandMark />
            </Box>
          </Group>
          <Box hiddenFrom={MOBILE_ONLY} px="xs">
            <QuickSearch onNavigate={closeMobileNav} />
          </Box>
          <Stack gap={2} mt="xs">
            <Text size="xs" fw={500} c="dimmed" px="sm" py="xs">
              {t('nav.panel')}
            </Text>
            <PanelNav onNavigate={closeMobileNav} />
          </Stack>
        </AppShell.Navbar>
        {isMobileNavOpen && <NavBackdrop onClose={closeMobileNav} />}
        <AppShell.Main pb={HELP_CLEARANCE_PX}>
          <Outlet />
        </AppShell.Main>
      </AppShell>
      <HelpAssistant />
    </>
  );
}
