import type { MantineColorsTuple } from '@mantine/core';
import { createTheme, rem } from '@mantine/core';

/** FE-UI-07: dokunmatik ekranlarda her etkileşimli öğe en az 44 px yüksekliğindedir. */
const TOUCH_TARGET_PX = 44;
const TOUCH_TARGET = rem(TOUCH_TARGET_PX);

/**
 * Mobil öncelikli tema: varsayılan boyutlar zaten dokunma hedefi sınırını geçer, böylece telefonda
 * ve masaüstünde aynı bileşenler kullanılır. Saha ekranları güneş altında da okunmalıdır (yüksek kontrast).
 */
const BRAND_COLORS: MantineColorsTuple = [
  '#eef6fb',
  '#d9ebf5',
  '#b3d6ea',
  '#85bcdb',
  '#5aa3cc',
  '#3b8dbd',
  '#2b78a8',
  '#1f6491',
  '#17527a',
  '#0f3f5f',
];

/** Beyaz metin için en az 4.5:1 kontrast veren koyu ton (WCAG AA). */
const BRAND_SHADE = 7;

export const theme = createTheme({
  colors: { brand: BRAND_COLORS },
  primaryColor: 'brand',
  primaryShade: BRAND_SHADE,
  defaultRadius: 'md',
  fontFamily: '"Inter Variable", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
  headings: { fontWeight: '700' },
  components: {
    InputWrapper: {
      styles: { label: { fontWeight: 600, marginBottom: 'var(--mantine-spacing-xs)' } },
    },
    Input: { styles: { input: { borderColor: 'var(--mantine-color-gray-5)' } } },
    Paper: { defaultProps: { radius: 'lg', shadow: 'xs' } },
    Badge: { defaultProps: { radius: 'sm', variant: 'light' } },
    Alert: { defaultProps: { radius: 'md', variant: 'light' } },
    Table: { defaultProps: { highlightOnHover: true, verticalSpacing: 'sm' } },
    AppShell: { styles: { header: { boxShadow: 'var(--mantine-shadow-xs)', borderBottom: 0 } } },
    Button: { defaultProps: { size: 'lg' } },
    TextInput: { defaultProps: { size: 'lg' } },
    Select: { defaultProps: { size: 'lg' } },
    Burger: {
      defaultProps: { size: 'md' },
      styles: { root: { minWidth: TOUCH_TARGET, minHeight: TOUCH_TARGET } },
    },
    NavLink: {
      styles: { root: { minHeight: TOUCH_TARGET, borderRadius: 'var(--mantine-radius-md)' } },
    },
    Tabs: { styles: { tab: { minHeight: TOUCH_TARGET } } },
    Stepper: { styles: { step: { minHeight: TOUCH_TARGET, minWidth: TOUCH_TARGET } } },
    Pagination: { styles: { control: { minWidth: TOUCH_TARGET, minHeight: TOUCH_TARGET } } },
    Anchor: {
      styles: { root: { display: 'inline-flex', alignItems: 'center', minHeight: TOUCH_TARGET } },
    },
  },
});
