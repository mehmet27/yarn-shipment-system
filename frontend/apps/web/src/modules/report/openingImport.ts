import type { OpeningImportRow } from '@iplik/api-client';

const COLUMN_COUNT = 3;
const KG_DECIMALS = 3;

export type ParsedLine = {
  line: number;
  raw: string;
  row?: OpeningImportRow;
  error?: 'format' | 'unknown' | 'kg';
};

/** `1.982,500` / `1982,5` / `1982.5` → "1982.500"; geçersizse undefined. */
export function parseNetKg(input: string): string | undefined {
  const value = input.trim().replace(/\s|kg$/gi, '');
  if (!/^-?\d[\d.,]*$/.test(value)) return undefined;
  let normalized = value;
  if (value.includes(',')) normalized = value.replace(/\./g, '').replace(',', '.');
  else if (/^-?\d{1,3}(\.\d{3})+$/.test(value)) normalized = value.replace(/\./g, '');
  const number = Number(normalized);
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
