import type { MantineColorsTuple, MantineThemeComponents } from '@mantine/core';
import { createTheme, rem, virtualColor } from '@mantine/core';
import {
  BRAND_SHADE_INDEX,
  DEFAULT_BRAND_COLOR,
  createBrandPalette,
  reversePalette,
} from './brandPalette';

/**
 * Nötr ölçek (çinko grisi): girinti (0), zemin vurgusu (1), çizgi (2-3), girdi kenarı (4), soluk yazı (6), ana yazı (9).
 * Renk yalnızca anlam taşıyan yerde kullanılır (durum, uyarı); gerisi bu ölçektedir.
 */
const ZINC: MantineColorsTuple = [
  '#fafafa',
  '#f4f4f5',
  '#e4e4e7',
  '#d4d4d8',
  '#a1a1aa',
  '#8a8a93',
  '#71717a',
  '#52525b',
  '#27272a',
  '#09090b',
];

/** Koyu görünüm ölçeği: yazı (0), soluk yazı (2), çizgi (4-5), kart (6), zemin (7), en koyu (9). */
const ZINC_DARK: MantineColorsTuple = [
  '#fafafa',
  '#d4d4d8',
  '#a1a1aa',
  '#71717a',
  '#3f3f46',
  '#27272a',
  '#18181b',
  '#09090b',
  '#050506',
  '#000000',
];

/** Ana eylem rengi açık görünümde: neredeyse siyah dolgu (7), üzerine gelince tam siyah (8). */
const INK_LIGHT: MantineColorsTuple = [
  '#fafafa',
  '#f4f4f5',
  '#e4e4e7',
  '#d4d4d8',
  '#a1a1aa',
  '#71717a',
  '#3f3f46',
  '#18181b',
  '#09090b',
  '#000000',
];

/** Ana eylem rengi koyu görünümde tersine döner: açık dolgu (7), koyu yazı. */
const INK_DARK: MantineColorsTuple = [
  '#18181b',
  '#27272a',
  '#3f3f46',
  '#52525b',
  '#71717a',
  '#a1a1aa',
  '#d4d4d8',
  '#fafafa',
  '#e4e4e7',
  '#d4d4d8',
];

/** Dolgu bu parlaklığın üstündeyse üzerindeki yazı koyu olur. */
const LUMINANCE_THRESHOLD = 0.4;

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
  const palette = createBrandPalette(brandColor);
  return createTheme({
    colors: {
      // Kurulumun rengi vurgu içindir (logo, etkin menü, bağlantı); koyu görünümde tonlar tersine döner.
      brandLight: palette,
      brandDark: reversePalette(palette),
      brand: virtualColor({ name: 'brand', light: 'brandLight', dark: 'brandDark' }),
      inkLight: INK_LIGHT,
      inkDark: INK_DARK,
      ink: virtualColor({ name: 'ink', light: 'inkLight', dark: 'inkDark' }),
      gray: ZINC,
      dark: ZINC_DARK,
    },
    black: ZINC[9],
    // Ana eylem düğmesi nötrdür (açıkta siyah, koyuda beyaz); yazı rengi dolguya göre kendiliğinden seçilir.
    primaryColor: 'ink',
    primaryShade: BRAND_SHADE_INDEX,
    autoContrast: true,
    luminanceThreshold: LUMINANCE_THRESHOLD,
    defaultRadius: 'md',
    radius: RADIUS,
    fontFamily: '"Geist Variable", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
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
        styles: {
          input: { borderColor: 'var(--app-control-line)', backgroundColor: 'var(--app-surface)' },
        },
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
