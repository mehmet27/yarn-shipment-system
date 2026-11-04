import type { MantineColorsTuple } from '@mantine/core';

/** Marka rengi kurulum başına bir kez seçilir; varsayılan lacivert-mavi. */
export const DEFAULT_BRAND_COLOR = '#17527a';

/** Beyaz metin için WCAG AA (normal metin) alt sınırı. */
const MIN_CONTRAST_ON_WHITE = 4.5;

/** `brand.7` düğme ve başlık zemini olarak kullanılır; bu sıradaki ton daima AA kontrastını sağlar. */
export const BRAND_SHADE_INDEX = 7;

type Rgb = readonly [number, number, number];

const HEX_PATTERN = /^#?([0-9a-f]{6})$/i;
const CHANNEL_MAX = 255;
const DARKEN_STEP = 0.04;
const MAX_DARKEN_STEPS = 25;
/** Açık tonlar: ana tonun beyaza karışım oranı (index 0–6). */
const LIGHT_MIX_RATIOS = [0.06, 0.14, 0.3, 0.5, 0.68, 0.84, 0.93] as const;
/** Koyu tonlar: ana tonun siyaha karışım oranı (index 8–9). */
const DARK_MIX_RATIOS = [0.16, 0.34] as const;
const BLACK: Rgb = [0, 0, 0];
const WHITE: Rgb = [CHANNEL_MAX, CHANNEL_MAX, CHANNEL_MAX];

/** `#rrggbb` (ya da `rrggbb`) biçimini doğrular. */
export function isValidHexColor(value: string): boolean {
  return HEX_PATTERN.test(value);
}

function parseHex(hex: string): Rgb {
  const match = HEX_PATTERN.exec(hex);
  if (match === null) {
    throw new Error(`Geçersiz renk: ${hex}`);
  }
  const value = Number.parseInt(match[1] ?? '', 16);
  return [(value >> 16) & CHANNEL_MAX, (value >> 8) & CHANNEL_MAX, value & CHANNEL_MAX];
}

function toHex([r, g, b]: Rgb): string {
  return `#${[r, g, b].map((c) => Math.round(c).toString(16).padStart(2, '0')).join('')}`;
}

function mix(from: Rgb, to: Rgb, ratio: number): Rgb {
  return [
    from[0] + (to[0] - from[0]) * ratio,
    from[1] + (to[1] - from[1]) * ratio,
    from[2] + (to[2] - from[2]) * ratio,
  ];
}

const SRGB_THRESHOLD = 0.03928;
const SRGB_OFFSET = 0.055;
const SRGB_DIVISOR = 1.055;
const SRGB_GAMMA = 2.4;
const LINEAR_DIVISOR = 12.92;
const LUMA_R = 0.2126;
const LUMA_G = 0.7152;
const LUMA_B = 0.0722;
const CONTRAST_OFFSET = 0.05;

function luminance([r, g, b]: Rgb): number {
  const linear = (channel: number): number => {
    const s = channel / CHANNEL_MAX;
    return s <= SRGB_THRESHOLD
      ? s / LINEAR_DIVISOR
      : ((s + SRGB_OFFSET) / SRGB_DIVISOR) ** SRGB_GAMMA;
  };
  return LUMA_R * linear(r) + LUMA_G * linear(g) + LUMA_B * linear(b);
}

/** İki renk arasındaki WCAG kontrast oranı (1–21). */
export function contrastRatio(a: Rgb, b: Rgb): number {
  const first = luminance(a);
  const second = luminance(b);
  return (Math.max(first, second) + CONTRAST_OFFSET) / (Math.min(first, second) + CONTRAST_OFFSET);
}

/** Seçilen rengi, beyaz üstünde AA kontrastı sağlayana kadar tonunu bozmadan koyulaştırır. */
function ensureReadableOnWhite(base: Rgb): Rgb {
  let color = base;
  for (let i = 0; i < MAX_DARKEN_STEPS; i += 1) {
    if (contrastRatio(color, WHITE) >= MIN_CONTRAST_ON_WHITE) {
      return color;
    }
    color = mix(color, BLACK, DARKEN_STEP);
  }
  return color;
}

/**
 * Tek marka renginden Mantine'in 10 tonluk paletini üretir. `BRAND_SHADE_INDEX` sırasındaki ton
 * seçilen renktir (gerekirse okunabilirlik için koyulaştırılır); açık tonlar beyaza, koyu tonlar
 * siyaha karıştırılarak elde edilir. Geçersiz değerde `DEFAULT_BRAND_COLOR` kullanılır.
 */
export function createBrandPalette(hex: string): MantineColorsTuple {
  const base = parseHex(isValidHexColor(hex) ? hex : DEFAULT_BRAND_COLOR);
  const main = ensureReadableOnWhite(base);
  const lighter = (ratio: number): string => toHex(mix(WHITE, main, ratio));
  const darker = (ratio: number): string => toHex(mix(main, BLACK, ratio));
  return [
    lighter(LIGHT_MIX_RATIOS[0]),
    lighter(LIGHT_MIX_RATIOS[1]),
    lighter(LIGHT_MIX_RATIOS[2]),
    lighter(LIGHT_MIX_RATIOS[3]),
    lighter(LIGHT_MIX_RATIOS[4]),
    lighter(LIGHT_MIX_RATIOS[5]),
    lighter(LIGHT_MIX_RATIOS[6]),
    toHex(main),
    darker(DARK_MIX_RATIOS[0]),
    darker(DARK_MIX_RATIOS[1]),
  ];
}
