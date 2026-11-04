import { createTheme, rem } from '@mantine/core';
import { BRAND_SHADE_INDEX, DEFAULT_BRAND_COLOR, createBrandPalette } from './brandPalette';

/** FE-UI-07: dokunmatik ekranlarda her etkileşimli öğe en az 44 px yüksekliğindedir. */
const TOUCH_TARGET_PX = 44;
const TOUCH_TARGET = rem(TOUCH_TARGET_PX);

/**
 * Mobil öncelikli tema: varsayılan boyutlar zaten dokunma hedefi sınırını geçer, böylece telefonda
 * ve masaüstünde aynı bileşenler kullanılır. Saha ekranları güneş altında da okunmalıdır (yüksek kontrast).
 * Marka rengi tek bir hex değerinden üretilir; her kurulum kendi rengini verir.
 */
export function createAppTheme(brandColor: string = DEFAULT_BRAND_COLOR) {
  return createTheme({
    colors: { brand: createBrandPalette(brandColor) },
    primaryColor: 'brand',
    primaryShade: BRAND_SHADE_INDEX,
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
      // Dar ekranda uzun etiketler kesilmesin: etiket satır atlar, düğme yüksekliği uzar.
      Button: {
        defaultProps: { size: 'lg' },
        // Yükseklik auto olunca iç kutu kendi boyunda kalır ve yazı üste yapışır; kök flex olup
        // içeriği dikeyde ortalar (H-26).
        styles: {
          root: {
            height: 'auto',
            minHeight: 'var(--button-height)',
            paddingBlock: 8,
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
          },
          inner: { flex: 1 },
          label: { whiteSpace: 'normal', textAlign: 'center' },
        },
      },
      TextInput: { defaultProps: { size: 'lg' } },
      Select: { defaultProps: { size: 'lg' } },
      // Artı/eksi okları 44 px'e sığmaz; telefonda sayı klavyesi açılır.
      NumberInput: { defaultProps: { size: 'lg', hideControls: true } },
      Checkbox: { vars: () => ({ root: { '--checkbox-size': TOUCH_TARGET } }) },
      Burger: {
        defaultProps: { size: 'md' },
        styles: { root: { minWidth: TOUCH_TARGET, minHeight: TOUCH_TARGET } },
      },
      Switch: {
        styles: {
          body: { minHeight: TOUCH_TARGET, alignItems: 'center' },
          root: { minHeight: TOUCH_TARGET },
        },
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
}

export const theme = createAppTheme();
