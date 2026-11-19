import { Can, useCurrentUser } from '@/modules/auth';
import type { Permission } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import type { NavIconName } from '@iplik/ui';
import { NavIcon, NavLink, Text } from '@iplik/ui';
import { NavLink as RouterNavLink, useLocation } from 'react-router';

type PanelNavGroup = 'daily' | 'setup';

type PanelNavItem = {
  readonly group: PanelNavGroup;
  readonly to: string;
  readonly labelKey: string;
  readonly icon: NavIconName;
  readonly permission?: Permission;
};

const ITEMS: readonly PanelNavItem[] = [
  {
    group: 'daily',
    to: '/panel/sevkiyat',
    labelKey: 'nav.shipments',
    icon: 'shipments',
    permission: 'SHIPMENT_VIEW',
  },
  {
    group: 'daily',
    to: '/panel/merkez-stok',
    labelKey: 'nav.centralStock',
    icon: 'balances',
    permission: 'CENTRAL_STOCK_VIEW',
  },
  {
    group: 'daily',
    to: '/panel/bakiye',
    labelKey: 'nav.balances',
    icon: 'balances',
    permission: 'BALANCE_VIEW',
  },
  {
    group: 'daily',
    to: '/panel/faturalama',
    labelKey: 'nav.invoicing',
    icon: 'invoices',
    permission: 'INVOICE_MARK',
  },
  {
    group: 'daily',
    to: '/panel/uyusmazliklar',
    labelKey: 'nav.discrepancies',
    icon: 'discrepancies',
    permission: 'DISCREPANCY_VIEW',
  },
  {
    group: 'daily',
    to: '/panel/raporlar',
    labelKey: 'nav.reports',
    icon: 'reports',
    permission: 'REPORT_VIEW',
  },
  {
    group: 'setup',
    to: '/panel/tanimlar',
    labelKey: 'nav.masterData',
    icon: 'masterData',
    permission: 'MASTER_DATA_MANAGE',
  },
  {
    group: 'setup',
    to: '/panel/partiler',
    labelKey: 'nav.lots',
    icon: 'lots',
    permission: 'LOT_QUALITY_LOCK',
  },
  {
    group: 'setup',
    to: '/panel/kullanicilar',
    labelKey: 'nav.users',
    icon: 'users',
    permission: 'USER_MANAGE',
  },
  {
    group: 'setup',
    to: '/panel/yetkiler',
    labelKey: 'nav.permissions',
    icon: 'permissions',
    permission: 'PERMISSION_MANAGE',
  },
  {
    group: 'setup',
    to: '/panel/gorunum',
    labelKey: 'nav.branding',
    icon: 'branding',
    permission: 'SETTINGS_MANAGE',
  },
  {
    group: 'setup',
    to: '/panel/denetim',
    labelKey: 'nav.audit',
    icon: 'audit',
    permission: 'AUDIT_VIEW',
  },
];

const NAV_LINK_STYLES = { label: { fontWeight: 500 } } as const;

export type PanelNavProps = {
  /** Bir bağlantıya tıklanınca çağrılır; mobilde menüyü kapatmak için. */
  readonly onNavigate: () => void;
};

const GROUPS: readonly PanelNavGroup[] = ['daily', 'setup'];

/**
 * Panel yan menüsü: her bağlantı yalnızca yetkisi olan kullanıcıya görünür. Menü "günlük işler" ve "tanımlar ve ayarlar"
 * diye iki başlığa ayrılır; başlıklar yalnızca kullanıcı iki gruptan da bir şey görüyorsa yazılır (depocuda tek liste kalır).
 */
export function PanelNav({ onNavigate }: PanelNavProps) {
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const { data: user } = useCurrentUser();
  const allowed = (item: PanelNavItem) =>
    item.permission === undefined || (user?.permissions.includes(item.permission) ?? false);
  const visibleGroups = GROUPS.filter((group) =>
    ITEMS.some((item) => item.group === group && allowed(item)),
  );
  return visibleGroups.map((group) => (
    <div key={group}>
      {visibleGroups.length > 1 && (
        <Text size="xs" fw={500} c="dimmed" px="sm" pt="md" pb={4}>
          {t(`nav.groups.${group}`)}
        </Text>
      )}
      {ITEMS.filter((item) => item.group === group).map((item) => {
        const link = (
          <NavLink
            key={item.to}
            component={RouterNavLink}
            to={item.to}
            onClick={onNavigate}
            label={t(item.labelKey)}
            leftSection={<NavIcon name={item.icon} />}
            active={pathname.startsWith(item.to)}
            className="panel-nav-link"
            variant="subtle"
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
      })}
    </div>
  ));
}
