export type NavIconName =
  | 'shipments'
  | 'reports'
  | 'discrepancies'
  | 'masterData'
  | 'lots'
  | 'settings'
  | 'branding'
  | 'audit'
  | 'users'
  | 'permissions'
  | 'balances'
  | 'invoices'
  | 'bell'
  | 'password'
  | 'logout'
  | 'camera'
  | 'upload'
  | 'file'
  | 'pdf'
  | 'excel'
  | 'download'
  | 'plus'
  | 'check'
  | 'arrowRight'
  | 'arrowLeft'
  | 'close'
  | 'edit'
  | 'calendar'
  | 'trash'
  | 'photo'
  | 'search'
  | 'info'
  | 'list'
  | 'user'
  | 'truck'
  | 'stop'
  | 'refresh'
  | 'save'
  | 'print'
  | 'eye'
  | 'sign';

/** Her ikon 24x24 çizgi (stroke) yolu; renk metinden (currentColor) gelir. */
const PATHS: Record<NavIconName, string> = {
  shipments: 'M1 6h13v10H1zM14 10h5l3 3v3h-8M6 18.5a1.5 1.5 0 100 .01M17 18.5a1.5 1.5 0 100 .01',
  reports: 'M4 20V10M10 20V4M16 20v-8M22 20H2',
  discrepancies: 'M12 3l10 18H2zM12 10v5M12 18v.5',
  masterData:
    'M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3-3.6 3-8 3-8-1.3-8-3zM4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6',
  lots: 'M12 3l9 5-9 5-9-5zM3 13l9 5 9-5M3 17l9 5 9-5',
  settings: 'M4 7h10M18 7h2M4 17h2M10 17h10M16 4v6M8 14v6',
  branding:
    'M12 3a9 9 0 100 18c1.2 0 2-.8 2-1.8 0-.6-.3-1-.6-1.4-.3-.4-.4-.8-.4-1.3 0-1 .8-1.8 1.8-1.8H17a4 4 0 004-4c0-4.4-4-7.7-9-7.7zM7.5 11.5a1 1 0 100 .01M10 7.5a1 1 0 100 .01M14.5 7.5a1 1 0 100 .01',
  audit: 'M7 3h8l4 4v14H7zM15 3v4h4M10 12h6M10 16h6',
  users:
    'M9 11a4 4 0 100-8 4 4 0 000 8zM2 21c0-4 3-6 7-6s7 2 7 6M17 3.5a4 4 0 010 7M22 21c0-3-1.5-5-4-5.7',
  permissions: 'M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6zM9 12l2 2 4-4',
  balances: 'M12 3v18M5 7h14M5 7l-3 7a3 3 0 006 0zM19 7l-3 7a3 3 0 006 0z',
  invoices: 'M6 3h12v18l-3-2-3 2-3-2-3 2zM9 8h6M9 12h6',
  bell: 'M6 8a6 6 0 0112 0c0 7 3 9 3 9H3s3-2 3-9M10 21a2 2 0 004 0',
  password: 'M3 11h18v10H3zM7 11V7a5 5 0 0110 0v4M12 15v2',
  logout: 'M9 21H4V3h5M16 17l5-5-5-5M21 12H9',
  camera:
    'M3 8a2 2 0 012-2h2l1.5-2h7L17 6h2a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2zM12 17a4 4 0 100-8 4 4 0 000 8z',
  upload: 'M12 16V4M7 9l5-5 5 5M4 20h16',
  file: 'M7 3h8l4 4v14H7zM15 3v4h4M10 12h6M10 16h6',
  pdf: 'M7 3h8l4 4v14H7zM15 3v4h4M9.5 17v-4h1.5a1.2 1.2 0 010 2.4H9.5M14 17v-4M14 13h2',
  excel: 'M4 4h16v16H4zM4 10h16M4 15h16M10 4v16',
  download: 'M12 4v12M7 11l5 5 5-5M4 20h16',
  plus: 'M12 5v14M5 12h14',
  check: 'M5 12.5l4.5 4.5L19 7.5',
  arrowRight: 'M5 12h14M13 6l6 6-6 6',
  arrowLeft: 'M19 12H5M11 6l-6 6 6 6',
  close: 'M6 6l12 12M18 6L6 18',
  edit: 'M4 20h4L19 9l-4-4L4 16zM13 7l4 4',
  calendar: 'M4 6h16v14H4zM4 10h16M8 3v4M16 3v4',
  trash: 'M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3',
  photo: 'M4 5h16v14H4zM4 16l5-5 4 4 3-3 4 4M9 9a1 1 0 100 .01',
  search: 'M11 4a7 7 0 100 14 7 7 0 000-14zM20 20l-4-4',
  info: 'M12 3a9 9 0 100 18 9 9 0 000-18zM12 11v5M12 8v.5',
  list: 'M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01',
  user: 'M12 11a4 4 0 100-8 4 4 0 000 8zM4 21c0-4 3-6 8-6s8 2 8 6',
  truck: 'M1 6h13v10H1zM14 10h5l3 3v3h-8M6 18.5a1.5 1.5 0 100 .01M17 18.5a1.5 1.5 0 100 .01',
  stop: 'M12 3a9 9 0 100 18 9 9 0 000-18zM6 6l12 12',
  refresh: 'M4 4v6h6M20 20v-6h-6M5 14a8 8 0 0014 3M19 10A8 8 0 005 7',
  save: 'M5 3h12l3 3v15H5zM8 3v6h8V3M8 21v-7h8v7',
  print: 'M7 9V3h10v6M7 17H4v-6h16v6h-3M7 14h10v7H7z',
  eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zM12 15a3 3 0 100-6 3 3 0 000 6z',
  sign: 'M4 20h16M6 16l3-7 3 5 2-3 4 5',
};

const ICON_SIZE_PX = 20;

export type NavIconProps = {
  name: NavIconName;
  /** Piksel; varsayılan 20 (menü). Düğme içinde 16 kullanılır. */
  size?: number;
};

/** Menü ikonu: dekoratif olduğu için ekran okuyucudan gizlenir (etiket zaten metinle verilir). */
export function NavIcon({ name, size = ICON_SIZE_PX }: NavIconProps) {
  return (
    <svg
      width={size}
      height={size}
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
