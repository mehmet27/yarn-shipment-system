import type { MantineColorsTuple, MantineThemeComponents } from '@mantine/core';
import { createTheme, rem } from '@mantine/core';
import { BRAND_SHADE_INDEX, DEFAULT_BRAND_COLOR, createBrandPalette } from './brandPalette';

/**
 * Nötr ölçek: soğuk taş grisi. Zemin (1), kart içi girinti (0), çizgi (2-3), girdi kenarı (5), soluk yazı (7),
 * ana yazı (9). Renk yalnızca anlam taşıyan yerde kullanılır (durum, uyarı, ana eylem); gerisi bu ölçektedir.
 */
const STONE: MantineColorsTuple = [
  '#f6f7f9',
  '#eceef2',
  '#e2e5ea',
  '#d3d8df',
  '#bcc3cd',
  '#959eab',
  '#6c7684',
  '#4f5967',
  '#333b47',
  '#18202a',
];

/** Köşe yuvarlaklığı: denetimler 8 px, kartlar 8 px, pencereler 12 px; her şeye aynı yuvarlaklık verilmez. */
const RADIUS = { xs: '0.25rem', sm: '0.375rem', md: '0.5rem', lg: '0.75rem', xl: '1rem' } as const;

/** Başlık ölçeği: sayfa başlığı 24 px, bölüm 20 px, alt bölüm 17 px; hepsi yarı kalın. */
const HEADINGS = {
  fontWeight: '600',
  sizes: {
    h1: { fontSize: '1.5rem', lineHeight: '1.25' },
    h2: { fontSize: '1.25rem', lineHeight: '1.3' },
    h3: { fontSize: '1.0625rem', lineHeight: '1.35' },
    h4: { fontSize: '0.9375rem', lineHeight: '1.4' },
  },
} as const;

/** FE-UI-07: dokunmatik ekranlarda her etkileşimli öğe en az 44 px yüksekliğindedir. */
const TOUCH_TARGET_PX = 44;
const TOUCH_TARGET = rem(TOUCH_TARGET_PX);

/** Dokunmatik ekranda en az 44 px olması gereken bileşenlerin boyut ayarları. */
const TOUCH_COMPONENTS: MantineThemeComponents = {
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
  // Sekmeler alt çizgili: etkin sekme marka renginde çizgiyle belirir; dar ekranda yana kayar (styles.css).
  Tabs: { defaultProps: { variant: 'default' }, styles: { tab: { minHeight: TOUCH_TARGET } } },
  Stepper: { styles: { step: { minHeight: TOUCH_TARGET, minWidth: TOUCH_TARGET } } },
  Pagination: { styles: { control: { minWidth: TOUCH_TARGET, minHeight: TOUCH_TARGET } } },
  Anchor: {
    styles: { root: { display: 'inline-flex', alignItems: 'center', minHeight: TOUCH_TARGET } },
  },
};

/** Düğme ve giriş alanlarının varsayılanları. */
const FORM_COMPONENTS: MantineThemeComponents = {
  // Dar ekranda uzun etiketler kesilmesin: etiket satır atlar, düğme yüksekliği uzar.
  Button: {
    defaultProps: { size: 'md' },
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
  // Boyut `md`: telefonda 44 px, masaüstünde 36 px (styles.css `--app-control-height`).
  TextInput: { defaultProps: { size: 'md' } },
  PasswordInput: { defaultProps: { size: 'md' } },
  Textarea: { defaultProps: { size: 'md' } },
  Select: { defaultProps: { size: 'md' } },
  // Artı/eksi okları 44 px'e sığmaz; telefonda sayı klavyesi açılır.
  NumberInput: { defaultProps: { size: 'md', hideControls: true } },
};

/**
 * Mobil öncelikli tema: varsayılan boyutlar zaten dokunma hedefi sınırını geçer, böylece telefonda
 * ve masaüstünde aynı bileşenler kullanılır. Saha ekranları güneş altında da okunmalıdır (yüksek kontrast).
 * Marka rengi tek bir hex değerinden üretilir; her kurulum kendi rengini verir.
 */
export function createAppTheme(brandColor: string = DEFAULT_BRAND_COLOR) {
  return createTheme({
    colors: { brand: createBrandPalette(brandColor), gray: STONE },
    black: STONE[9],
    primaryColor: 'brand',
    primaryShade: BRAND_SHADE_INDEX,
    defaultRadius: 'md',
    radius: RADIUS,
    fontFamily:
      '"IBM Plex Sans Variable", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
    headings: HEADINGS,
    components: {
      InputWrapper: {
        styles: {
          label: {
            fontWeight: 500,
            fontSize: 'var(--mantine-font-size-sm)',
            marginBottom: '0.25rem',
          },
        },
      },
      Input: {
        styles: { input: { borderColor: 'var(--mantine-color-gray-5)', backgroundColor: 'white' } },
      },
      // Kart gölgesiz, ince çizgili: gölge yalnızca üstte duran katmanlarda (menü, pencere, sabit eylem çubuğu) kalır.
      Paper: { defaultProps: { radius: 'md', withBorder: true } },
      Badge: { defaultProps: { radius: 'sm', variant: 'light' } },
      Modal: { defaultProps: { radius: 'lg', centered: true } },
      Alert: { defaultProps: { radius: 'md', variant: 'light' } },
      Table: {
        defaultProps: {
          highlightOnHover: true,
          verticalSpacing: 'xs',
          withRowBorders: true,
        },
      },
      AppShell: { styles: { header: { borderBottom: 0 } } },
      ...FORM_COMPONENTS,
      ...TOUCH_COMPONENTS,
    },
  });
}

export const theme = createAppTheme();
