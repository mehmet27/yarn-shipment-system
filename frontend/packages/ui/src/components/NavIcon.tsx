export type NavIconName =
  | 'shipments'
  | 'bags'
  | 'reports'
  | 'discrepancies'
  | 'masterData'
  | 'lots'
  | 'settings'
  | 'audit'
  | 'users';

/** Her ikon 24x24 çizgi (stroke) yolu; renk metinden (currentColor) gelir. */
const PATHS: Record<NavIconName, string> = {
  shipments: 'M1 6h13v10H1zM14 10h5l3 3v3h-8M6 18.5a1.5 1.5 0 100 .01M17 18.5a1.5 1.5 0 100 .01',
  bags: 'M5 8h14l-1 12H6zM9 8V6a3 3 0 016 0v2',
  reports: 'M4 20V10M10 20V4M16 20v-8M22 20H2',
  discrepancies: 'M12 3l10 18H2zM12 10v5M12 18v.5',
  masterData:
    'M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6',
  lots: 'M12 3l9 5-9 5-9-5zM3 13l9 5 9-5M3 17l9 5 9-5',
  settings: 'M4 7h10M18 7h2M4 17h2M10 17h10M16 4v6M8 14v6',
  audit: 'M7 3h8l4 4v14H7zM15 3v4h4M10 12h6M10 16h6',
  users:
    'M9 11a4 4 0 100-8 4 4 0 000 8zM2 21c0-4 3-6 7-6s7 2 7 6M17 3.5a4 4 0 010 7M22 21c0-3-1.5-5-4-5.7',
};

const ICON_SIZE_PX = 20;

export type NavIconProps = {
  name: NavIconName;
};

/** Menü ikonu: dekoratif olduğu için ekran okuyucudan gizlenir (etiket zaten metinle verilir). */
export function NavIcon({ name }: NavIconProps) {
  return (
    <svg
      width={ICON_SIZE_PX}
      height={ICON_SIZE_PX}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d={PATHS[name]} />
    </svg>
  );
}
