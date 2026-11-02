import { Can } from '@/modules/auth';
import type { Permission } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { NavLink } from '@iplik/ui';
import { NavLink as RouterNavLink, useLocation } from 'react-router';

type PanelNavItem = {
  readonly to: string;
  readonly labelKey: string;
  readonly permission?: Permission;
};

const ITEMS: readonly PanelNavItem[] = [
  { to: '/panel/sevkiyat', labelKey: 'nav.shipments' },
  { to: '/panel/cuvallar', labelKey: 'nav.bags', permission: 'BAG_CREATE' },
  { to: '/panel/uyusmazliklar', labelKey: 'nav.discrepancies', permission: 'DISCREPANCY_VIEW' },
  { to: '/panel/tanimlar', labelKey: 'nav.masterData', permission: 'MASTER_DATA_MANAGE' },
  { to: '/panel/partiler', labelKey: 'nav.lots', permission: 'LOT_QUALITY_LOCK' },
  {
    to: '/panel/kural-ayarlari',
    labelKey: 'nav.discrepancySettings',
    permission: 'SETTINGS_MANAGE',
  },
];

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
        active={pathname.startsWith(item.to)}
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
