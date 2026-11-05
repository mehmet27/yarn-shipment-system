import { Can } from '@/modules/auth';
import type { Permission } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import type { NavIconName } from '@iplik/ui';
import { NavIcon, NavLink } from '@iplik/ui';
import { NavLink as RouterNavLink, useLocation } from 'react-router';

type PanelNavItem = {
  readonly to: string;
  readonly labelKey: string;
  readonly icon: NavIconName;
  readonly permission?: Permission;
};

const ITEMS: readonly PanelNavItem[] = [
  { to: '/panel/sevkiyat', labelKey: 'nav.shipments', icon: 'shipments' },
  { to: '/panel/cuvallar', labelKey: 'nav.bags', icon: 'bags', permission: 'BAG_CREATE' },
  { to: '/panel/raporlar', labelKey: 'nav.reports', icon: 'reports', permission: 'REPORT_VIEW' },
  {
    to: '/panel/uyusmazliklar',
    labelKey: 'nav.discrepancies',
    icon: 'discrepancies',
    permission: 'DISCREPANCY_VIEW',
  },
  {
    to: '/panel/tanimlar',
    labelKey: 'nav.masterData',
    icon: 'masterData',
    permission: 'MASTER_DATA_MANAGE',
  },
  { to: '/panel/partiler', labelKey: 'nav.lots', icon: 'lots', permission: 'LOT_QUALITY_LOCK' },
  {
    to: '/panel/kural-ayarlari',
    labelKey: 'nav.discrepancySettings',
    icon: 'settings',
    permission: 'SETTINGS_MANAGE',
  },
  {
    to: '/panel/gorunum',
    labelKey: 'nav.branding',
    icon: 'settings',
    permission: 'SETTINGS_MANAGE',
  },
  { to: '/panel/denetim', labelKey: 'nav.audit', icon: 'audit', permission: 'AUDIT_VIEW' },
  { to: '/panel/kullanicilar', labelKey: 'nav.users', icon: 'users', permission: 'USER_MANAGE' },
];

const NAV_LINK_STYLES = { label: { fontWeight: 500 } } as const;

/** Panel yan menüsü: her bağlantı yalnızca yetkisi olan kullanıcıya görünür. */
export function PanelNav() {
  const { t } = useTranslation();
  const { pathname } = useLocation();
  return ITEMS.map((item) => {
    const link = (
      <NavLink
        key={item.to}
        component={RouterNavLink}
        to={item.to}
        label={t(item.labelKey)}
        leftSection={<NavIcon name={item.icon} />}
        active={pathname.startsWith(item.to)}
        className="panel-nav-link"
        c="white"
        variant="filled"
        color="brand.6"
        styles={NAV_LINK_STYLES}
      />
    );
    return item.permission === undefined ? (
      link
    ) : (
      <Can key={item.to} permission={item.permission}>
        {link}
      </Can>
    );
  });
}
