import { Box, Group } from '@iplik/ui';
import { BrandMark } from '@/modules/branding';
import { QuickSearch } from '@/modules/shipment';
import { NavToggles } from './NavToggles';
import { NotificationBell } from './NotificationBell';
import { UserMenu } from './UserMenu';

/** Bu genişliğin altı telefon düzenidir. */
const MOBILE_ONLY = 'sm';

export type PanelHeaderProps = {
  isMobileNavOpen: boolean;
  isDesktopNavOpen: boolean;
  onToggleMobileNav: () => void;
  onToggleDesktopNav: () => void;
};

/**
 * Üst çubuk: solda yan menü düğmesi (telefonda firma adıyla), sağda hızlı arama, bekleyen işler zili ve kullanıcı
 * menüsü. Telefonda hızlı arama yer kaplamasın diye yan menünün içindedir.
 */
export function PanelHeader({
  isMobileNavOpen,
  isDesktopNavOpen,
  onToggleMobileNav,
  onToggleDesktopNav,
}: PanelHeaderProps) {
  return (
    <Group h="100%" px="md" justify="space-between" wrap="nowrap">
      <Group wrap="nowrap" miw={0}>
        <NavToggles
          isMobileOpen={isMobileNavOpen}
          isDesktopOpen={isDesktopNavOpen}
          onToggleMobile={onToggleMobileNav}
          onToggleDesktop={onToggleDesktopNav}
        />
        <Box hiddenFrom={MOBILE_ONLY} miw={0}>
          <BrandMark />
        </Box>
      </Group>
      <Group gap="xs" wrap="nowrap">
        <Box visibleFrom={MOBILE_ONLY}>
          <QuickSearch />
        </Box>
        <NotificationBell />
        <UserMenu />
      </Group>
    </Group>
  );
}
