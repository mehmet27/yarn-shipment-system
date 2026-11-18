import type { OpeningImportRow } from '@iplik/api-client';

const COLUMN_COUNT = 3;
const KG_DECIMALS = 3;

export type ParsedLine = {
  line: number;
  raw: string;
  row?: OpeningImportRow;
  error?: 'format' | 'unknown' | 'kg';
};

/** Türkçe kilo kuralı (`1.982,5`, `1982,5`, `1.500` = 1500, `1,5`); nokta yalnızca binlik ayırıcıdır. Geçersizse undefined. */
export function parseNetKg(input: string): string | undefined {
  const value = input.trim().replace(/\s*kg$/i, '');
  if (!/^(\d{1,3}(\.\d{3})+|\d+)(,\d{1,3})?$/.test(value)) return undefined;
  const number = Number(value.replaceAll('.', '').replace(',', '.'));
  return Number.isFinite(number) ? number.toFixed(KG_DECIMALS) : undefined;
}

/** 'fasoncu;iplik;kg' satırları (noktalı virgül ya da sekme); fasoncu adı eşleşmeyen satır hatalıdır. */
export function parseOpeningLines(
  input: string,
  idByName: (name: string) => string | undefined,
): ParsedLine[] {
  return input
    .split(/\r?\n/)
    .map((raw, index) => ({ raw: raw.trim(), line: index + 1 }))
    .filter(({ raw }) => raw !== '')
    .map(({ raw, line }): ParsedLine => {
      const parts = raw.split(/[;\t]/).map((part) => part.trim());
      const [name, yarnType, kgText] = parts;
      if (parts.length !== COLUMN_COUNT || !name || !yarnType || kgText === undefined) {
        return { line, raw, error: 'format' };
      }
      const netKg = parseNetKg(kgText);
      if (netKg === undefined) return { line, raw, error: 'kg' };
      const subcontractorId = idByName(name);
      if (subcontractorId === undefined) return { line, raw, error: 'unknown' };
      return { line, raw, row: { subcontractorId, subcontractorName: name, yarnType, netKg } };
    });
}
