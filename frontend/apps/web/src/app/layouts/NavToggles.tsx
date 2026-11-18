import { useTranslation } from '@iplik/i18n';
import { Burger } from '@iplik/ui';

export type NavTogglesProps = {
  readonly isMobileOpen: boolean;
  readonly isDesktopOpen: boolean;
  readonly onToggleMobile: () => void;
  readonly onToggleDesktop: () => void;
};

/** Üst çubuktaki menü düğmesi: telefonda çekmeceyi, masaüstünde yan menüyü açıp kapatır. */
export function NavToggles({
  isMobileOpen,
  isDesktopOpen,
  onToggleMobile,
  onToggleDesktop,
}: NavTogglesProps) {
  const { t } = useTranslation();
  const label = t('nav.toggle');
  return (
    <>
      <Burger opened={isMobileOpen} onClick={onToggleMobile} hiddenFrom="sm" aria-label={label} />
      <Burger
        opened={isDesktopOpen}
        onClick={onToggleDesktop}
        visibleFrom="sm"
        aria-label={label}
      />
    </>
  );
}
