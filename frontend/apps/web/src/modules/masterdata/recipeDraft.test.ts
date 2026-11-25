import { describe, expect, it } from 'vitest';
import { recipeTotal, toRecipeRequest } from './recipeDraft';

const draft = (lines: [string, string][]) => ({
  fabricType: ' Süprem ',
  lines: lines.map(([yarnType, ratio]) => ({ yarnType, ratio })),
});

describe('toRecipeRequest', () => {
  it('toplamı 100 olan karışımı isteğe çevirir', () => {
    expect(
      toRecipeRequest(
        draft([
          ['Penye', '60,5'],
          ['Likra', '39.5'],
        ]),
      ),
    ).toEqual({
      fabricType: 'Süprem',
      yarns: [
        { yarnType: 'Penye', ratioPct: '60.5' },
        { yarnType: 'Likra', ratioPct: '39.5' },
      ],
    });
  });

  it('toplamı 100 olmayan, tekrarlı ya da eksik satırı reddeder', () => {
    expect(toRecipeRequest(draft([['Penye', '90']]))).toBeUndefined();
    expect(
      toRecipeRequest(
        draft([
          ['Penye', '50'],
          ['penye', '50'],
        ]),
      ),
    ).toBeUndefined();
    expect(toRecipeRequest(draft([['', '100']]))).toBeUndefined();
    expect(toRecipeRequest(draft([['Penye', '0']]))).toBeUndefined();
  });
});

describe('recipeTotal', () => {
  it('kayan nokta hatası olmadan toplar', () => {
    expect(
      recipeTotal(
        draft([
          ['A', '33,33'],
          ['B', '33,33'],
          ['C', '33,34'],
        ]),
      ),
    ).toBe(100);
  });
});
