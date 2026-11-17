import type { RecipeLineView, WeightDecisionLine } from '@iplik/api-client';
import { parseKg, subtractKg, sumKg } from '@iplik/units';

const KG_SCALE = 3;

/** Satır başına yazılan kilo metni (Türkçe girdi), satır numarasıyla. */
export type CorrectionDraft = Readonly<Record<number, string>>;

/** API kilosunu ("982.500") giriş kutusunda yazılacak Türkçe metne çevirir ("982,5"). */
function toInput(kg: string): string {
  return kg.replace(/\.?0+$/, '').replace('.', ',');
}

export function initialCorrection(lines: readonly RecipeLineView[]): CorrectionDraft {
  return Object.fromEntries(lines.map((line) => [line.lineNo, toInput(line.netKg)]));
}

/** Her satırın kilosu geçerliyse API satırları; biri geçersizse undefined. */
export function toCorrectionLines(
  lines: readonly RecipeLineView[],
  draft: CorrectionDraft,
): WeightDecisionLine[] | undefined {
  const parsed = lines.map((line) => ({
    lineNo: line.lineNo,
    netKg: parseKg(draft[line.lineNo] ?? ''),
  }));
  return parsed.every((line) => line.netKg !== undefined && Number(line.netKg) > 0)
    ? parsed.flatMap((line) =>
        line.netKg === undefined ? [] : [{ lineNo: line.lineNo, netKg: line.netKg }],
      )
    : undefined;
}

/** Yazılan kiloların toplamı ile tartılan net arasındaki fark ("0.000" ise eşit); satırlar geçersizse undefined. */
export function correctionDifference(
  corrected: readonly WeightDecisionLine[] | undefined,
  measuredKg: string,
): string | undefined {
  const measured = parseKg(toInput(measuredKg));
  if (corrected === undefined || measured === undefined) return undefined;
  const total = parseKg(toInput(sumKg(corrected.map((line) => line.netKg))));
  return total === undefined ? undefined : subtractKg(total, measured).toFixed(KG_SCALE);
}
