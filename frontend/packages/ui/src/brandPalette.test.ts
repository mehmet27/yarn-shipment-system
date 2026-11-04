import { describe, expect, it } from 'vitest';
import {
  BRAND_SHADE_INDEX,
  DEFAULT_BRAND_COLOR,
  contrastRatio,
  createBrandPalette,
  isValidHexColor,
} from './brandPalette';

const WHITE = [255, 255, 255] as const;

function toRgb(hex: string): readonly [number, number, number] {
  const value = Number.parseInt(hex.slice(1), 16);
  return [Math.floor(value / 65536) % 256, Math.floor(value / 256) % 256, value % 256];
}

describe('createBrandPalette', () => {
  it('10 ton üretir', () => {
    expect(createBrandPalette('#2563eb')).toHaveLength(10);
  });

  it.each(['#ffeb3b', '#ffffff', '#f59e0b', '#2563eb', '#000000', '#17527a'])(
    '%s için ana ton beyaz üstünde AA kontrastını sağlar',
    (color) => {
      const palette = createBrandPalette(color);
      expect(contrastRatio(toRgb(palette[BRAND_SHADE_INDEX]), WHITE)).toBeGreaterThanOrEqual(4.5);
    },
  );

  it('zaten koyu bir renk değişmeden ana ton olur', () => {
    expect(createBrandPalette('#17527a')[BRAND_SHADE_INDEX]).toBe('#17527a');
  });

  it('tonlar açıktan koyuya doğru sıralanır', () => {
    const palette = createBrandPalette('#2563eb');
    const lightness = palette.map((hex) => {
      const [r, g, b] = toRgb(hex);
      return r + g + b;
    });
    expect([...lightness].sort((a, b) => b - a)).toEqual(lightness);
  });

  it('geçersiz değerde varsayılan renge döner', () => {
    expect(createBrandPalette('mavi')).toEqual(createBrandPalette(DEFAULT_BRAND_COLOR));
  });

  it('# işareti olmadan da kabul eder', () => {
    expect(createBrandPalette('2563eb')).toEqual(createBrandPalette('#2563eb'));
  });
});

describe('isValidHexColor', () => {
  it('altı haneli hex kabul eder, diğerlerini reddeder', () => {
    expect(isValidHexColor('#a1B2c3')).toBe(true);
    expect(isValidHexColor('#abc')).toBe(false);
    expect(isValidHexColor('red')).toBe(false);
  });
});
