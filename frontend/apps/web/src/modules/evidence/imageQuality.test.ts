import { describe, expect, it } from 'vitest';
import { analyseLuma } from './imageQuality';

const SIZE = 8;
const CHANNELS = 4;
const MAX = 255;

function image(valueAt: (x: number, y: number) => number): Uint8ClampedArray {
  const data = new Uint8ClampedArray(SIZE * SIZE * CHANNELS);
  for (let i = 0; i < SIZE * SIZE; i++) {
    const value = valueAt(i % SIZE, Math.floor(i / SIZE));
    data.set([value, value, value, MAX], i * CHANNELS);
  }
  return data;
}

describe('analyseLuma', () => {
  it('düz görüntüyü bulanık sayar', () => {
    expect(
      analyseLuma(
        image(() => 100),
        SIZE,
        SIZE,
      ),
    ).toEqual(['BLURRY']);
  });

  it('aşırı parlak görüntüde parlama uyarır', () => {
    expect(
      analyseLuma(
        image(() => MAX),
        SIZE,
        SIZE,
      ),
    ).toContain('GLARE');
  });

  it('keskin kenarlı görüntüyü temiz sayar', () => {
    expect(
      analyseLuma(
        image((x, y) => ((x + y) % 2) * 200),
        SIZE,
        SIZE,
      ),
    ).toEqual([]);
  });
});
