export type QualityIssue = 'BLURRY' | 'GLARE';

const SAMPLE_WIDTH = 160;
const MIN_SAMPLE_HEIGHT = 3;
const BLUR_VARIANCE_MIN = 50;
const GLARE_LUMA = 252;
const GLARE_SHARE_MAX = 0.2;
const CHANNELS = 4;
const RED_WEIGHT = 0.299;
const GREEN_WEIGHT = 0.587;
const BLUE_WEIGHT = 0.114;
const LUMA_WEIGHTS = [RED_WEIGHT, GREEN_WEIGHT, BLUE_WEIGHT] as const;
const CENTER_WEIGHT = 4;

function luma(data: Uint8ClampedArray, index: number): number {
  return LUMA_WEIGHTS.reduce(
    (sum, weight, channel) => sum + weight * (data[index + channel] ?? 0),
    0,
  );
}

type Stats = { count: number; mean: number; variance: number; brightShare: number };

function measure(grid: readonly number[], width: number, height: number): Stats {
  const sums = { laplace: 0, squares: 0, bright: 0, count: 0 };
  for (let y = 1; y < height - 1; y++) {
    for (let x = 1; x < width - 1; x++) {
      const at = (dx: number, dy: number) => grid[(y + dy) * width + x + dx] ?? 0;
      const laplace = at(0, -1) + at(0, 1) + at(-1, 0) + at(1, 0) - CENTER_WEIGHT * at(0, 0);
      sums.laplace += laplace;
      sums.squares += laplace * laplace;
      sums.bright += at(0, 0) >= GLARE_LUMA ? 1 : 0;
      sums.count++;
    }
  }
  const count = Math.max(sums.count, 1);
  const mean = sums.laplace / count;
  return {
    count: sums.count,
    mean,
    variance: sums.squares / count - mean * mean,
    brightShare: sums.bright / count,
  };
}

/** Laplace varyansı düşükse görüntü bulanıktır; çok parlak piksel payı yüksekse yansıma vardır. */
export function analyseLuma(
  data: Uint8ClampedArray,
  width: number,
  height: number,
): QualityIssue[] {
  const grid = Array.from({ length: width * height }, (_, i) => luma(data, i * CHANNELS));
  const stats = measure(grid, width, height);
  if (stats.count === 0) return [];
  return [
    ...(stats.variance < BLUR_VARIANCE_MIN ? (['BLURRY'] as const) : []),
    ...(stats.brightShare > GLARE_SHARE_MAX ? (['GLARE'] as const) : []),
  ];
}

/** Cihazda en iyi çaba kontrolü; tuval ya da görüntü çözme yoksa sessizce geçer. */
export async function checkImageQuality(file: File): Promise<QualityIssue[]> {
  if (typeof createImageBitmap !== 'function') return [];
  try {
    const bitmap = await createImageBitmap(file);
    const height = Math.max(
      MIN_SAMPLE_HEIGHT,
      Math.round((bitmap.height / bitmap.width) * SAMPLE_WIDTH),
    );
    const canvas = document.createElement('canvas');
    canvas.width = SAMPLE_WIDTH;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (context === null) return [];
    context.drawImage(bitmap, 0, 0, SAMPLE_WIDTH, height);
    return analyseLuma(context.getImageData(0, 0, SAMPLE_WIDTH, height).data, SAMPLE_WIDTH, height);
  } catch {
    return [];
  }
}
