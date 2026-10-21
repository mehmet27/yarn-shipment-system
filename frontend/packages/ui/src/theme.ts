import { createTheme, rem } from '@mantine/core';

/** FE-UI-07: dokunmatik ekranlarda her etkileşimli öğe en az 44 px yüksekliğindedir. */
const TOUCH_TARGET_PX = 44;
const TOUCH_TARGET = rem(TOUCH_TARGET_PX);

/**
 * Mobil öncelikli tema: varsayılan boyutlar zaten dokunma hedefi sınırını geçer, böylece telefonda
 * ve masaüstünde aynı bileşenler kullanılır. Saha ekranları güneş altında da okunmalıdır (yüksek kontrast).
 */
export const theme = createTheme({
  primaryColor: 'indigo',
  defaultRadius: 'md',
  fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
  components: {
    Button: { defaultProps: { size: 'lg' } },
    TextInput: { defaultProps: { size: 'lg' } },
    Select: { defaultProps: { size: 'lg' } },
    Burger: {
      defaultProps: { size: 'md' },
      styles: { root: { minWidth: TOUCH_TARGET, minHeight: TOUCH_TARGET } },
    },
    NavLink: { styles: { root: { minHeight: TOUCH_TARGET } } },
    Tabs: { styles: { tab: { minHeight: TOUCH_TARGET } } },
    Stepper: { styles: { step: { minHeight: TOUCH_TARGET, minWidth: TOUCH_TARGET } } },
    Pagination: { styles: { control: { minWidth: TOUCH_TARGET, minHeight: TOUCH_TARGET } } },
    Anchor: {
      styles: { root: { display: 'inline-flex', alignItems: 'center', minHeight: TOUCH_TARGET } },
    },
  },
});
