import type { FabricRecipe, FabricYarnOption } from '@iplik/api-client/fabric';
import { describe, expect, it } from 'vitest';
import { parseRatio } from '@iplik/units';
import { findRecipe, ratioTotal, recipeToRatios } from './fabricRatios';

const recipe: FabricRecipe = {
  id: 'r1',
  fabricType: 'Süprem',
  active: true,
  yarns: [
    { yarnType: 'Penye', ratioPct: '60' },
    { yarnType: 'Likra', ratioPct: '40' },
  ],
};

describe('parseRatio', () => {
  it('virgül ya da noktayı kabul eder, geçersizi reddeder', () => {
    expect(parseRatio('33,5')).toBe('33.5');
    expect(parseRatio(' 100 ')).toBe('100');
    expect(parseRatio('0')).toBeUndefined();
    expect(parseRatio('100,01')).toBeUndefined();
    expect(parseRatio('1,234')).toBeUndefined();
    expect(parseRatio('-5')).toBeUndefined();
  });
});

describe('ratioTotal', () => {
  it('kayan nokta hatası olmadan toplar', () => {
    expect(ratioTotal({ a: '33,33', b: '33,33', c: '33,34' })).toBe(100);
    expect(ratioTotal({ a: 'x', b: '10' })).toBe(10);
  });
});

describe('findRecipe', () => {
  it('kumaş cinsini büyük/küçük harf fark etmeden bulur', () => {
    expect(findRecipe([recipe], ' süprem ')).toBe(recipe);
    expect(findRecipe([recipe], 'Ribana')).toBeUndefined();
    expect(findRecipe([recipe], '')).toBeUndefined();
  });
});

describe('recipeToRatios', () => {
  const held = (
    yarnType: string,
    lotId: string | undefined,
    totalKg: string,
  ): FabricYarnOption => ({
    yarnType,
    ...(lotId === undefined ? {} : { lotId }),
    totalKg,
    ownKg: totalKg,
  });

  it('aynı iplik birden çok partideyse oranı eldeki kiloya göre böler, toplamı korur', () => {
    const ratios = recipeToRatios(recipe, [
      held('Penye', 'p1', '300.000'),
      held('Penye', 'p2', '100.000'),
      held('Likra', undefined, '10.000'),
    ]);
    expect(ratios).toEqual({ 'Penye|p1': '45', 'Penye|p2': '15', 'Likra|': '40' });
  });

  it('bölünce artan yüzde birlik en çok kiloluya eklenir', () => {
    const ratios = recipeToRatios({ ...recipe, yarns: [{ yarnType: 'Penye', ratioPct: '100' }] }, [
      held('Penye', 'a', '100.000'),
      held('Penye', 'b', '100.000'),
      held('Penye', 'c', '100.000'),
    ]);
    expect(Object.values(ratios).reduce((sum, r) => sum + Math.round(Number(r) * 100), 0)).toBe(
      10_000,
    );
  });

  it('fasoncuda olmayan ipliği yazmaz', () => {
    expect(recipeToRatios(recipe, [held('Penye', undefined, '10.000')])).toEqual({
      'Penye|': '60',
    });
  });
});
