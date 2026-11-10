import type { CreateShipmentRequest, RecipeLineInput } from '@iplik/api-client';
import type { Kilogram } from '@iplik/units';
import { parseFireRatePct, parseKg, sumKg } from '@iplik/units';
import type { MasterOption } from '../hooks/useMasterOptions';

/** Reçete satırı taslağı; kilo kullanıcının yazdığı Türkçe metindir. `key` yalnız liste anahtarıdır. */
export type RecipeLineDraft = {
  key: string;
  yarn: MasterOption | undefined;
  yarnText: string;
  lot: MasterOption | undefined;
  netKg: string;
};

/** Seçilen kayıtlar kimlik ve görünen ad birlikte tutulur; istek ikisini de ister. */
export type CreateDraft = {
  site: MasterOption | undefined;
  subcontractor: MasterOption | undefined;
  driver: MasterOption | undefined;
  vehicle: MasterOption | undefined;
  lines: readonly RecipeLineDraft[];
  /** Yüzde metni; `undefined` ayardaki varsayılanı kullanır. */
  fireRatePct: string | undefined;
};

let lineCounter = 0;

export function newLine(): RecipeLineDraft {
  lineCounter += 1;
  return {
    key: `line-${String(lineCounter)}`,
    yarn: undefined,
    yarnText: '',
    lot: undefined,
    netKg: '',
  };
}

export const EMPTY_CREATE_DRAFT: CreateDraft = {
  site: undefined,
  subcontractor: undefined,
  driver: undefined,
  vehicle: undefined,
  lines: [newLine()],
  fireRatePct: undefined,
};

/** i18n anahtarı (`create.errors.*`). */
export type CreateError =
  | 'siteRequired'
  | 'subcontractorRequired'
  | 'linesRequired'
  | 'lineYarnRequired'
  | 'lineKgInvalid'
  | 'fireRateInvalid';

export type CreateFieldErrors = Partial<
  Record<'site' | 'subcontractor' | 'lines' | 'fireRate', CreateError>
>;

const POSITIVE_KG = /[1-9]/;

/** Net kg geçerli ve 0'dan büyük olmalı (3 ondalık). */
export function parsePositiveKg(input: string): Kilogram | undefined {
  const kg = parseKg(input);
  return kg !== undefined && POSITIVE_KG.test(kg) ? kg : undefined;
}

export function lineError(line: RecipeLineDraft): CreateError | undefined {
  if (line.yarn === undefined && line.yarnText.trim() === '') return 'lineYarnRequired';
  return parsePositiveKg(line.netKg) === undefined ? 'lineKgInvalid' : undefined;
}

/** Tüm eksik zorunlu alanlar birden döner; hata mesajı ilgili alanın altında gösterilir. */
export function validateCreateFields(draft: CreateDraft, defaultFirePct = '4'): CreateFieldErrors {
  const firstLineError = draft.lines.map(lineError).find((error) => error !== undefined);
  return {
    ...(draft.site === undefined ? { site: 'siteRequired' as const } : {}),
    ...(draft.subcontractor === undefined
      ? { subcontractor: 'subcontractorRequired' as const }
      : {}),
    ...(draft.lines.length === 0 ? { lines: 'linesRequired' as const } : {}),
    ...(firstLineError === undefined ? {} : { lines: firstLineError }),
    ...(parseFireRatePct(draft.fireRatePct ?? defaultFirePct) === undefined
      ? { fireRate: 'fireRateInvalid' as const }
      : {}),
  };
}

/** İlk hatayı döner; hata yoksa undefined. */
export function validateCreate(draft: CreateDraft, defaultFirePct = '4'): CreateError | undefined {
  const errors = validateCreateFields(draft, defaultFirePct);
  return errors.site ?? errors.subcontractor ?? errors.lines ?? errors.fireRate;
}

/** Fasoncu değişince önceki fasoncunun şoförü seçili kalmaz. */
export function withSubcontractor(draft: CreateDraft, subcontractor: MasterOption | undefined) {
  return { ...draft, subcontractor, driver: undefined };
}

/** Geçerli satırların net toplamı; geçersiz satırlar sayılmaz. */
export function totalNetKg(lines: readonly RecipeLineDraft[]): string {
  return sumKg(lines.map((line) => parsePositiveKg(line.netKg) ?? ''));
}

function toRecipeLine(line: RecipeLineDraft): RecipeLineInput | undefined {
  const netKg = parsePositiveKg(line.netKg);
  const yarnTypeText = line.yarnText.trim();
  if (netKg === undefined) return undefined;
  if (line.yarn === undefined && yarnTypeText === '') return undefined;
  return {
    netKg,
    ...(line.yarn === undefined ? { yarnTypeText } : { yarnTypeId: line.yarn.value }),
    ...(line.lot === undefined ? {} : { lotId: line.lot.value }),
  };
}

/** Boş bırakılan şoför ve araç istekte hiç yer almaz; fire oranı kesirdir (%4 → 0.04). */
export function toCreateRequest(
  draft: CreateDraft,
  defaultFirePct = '4',
): CreateShipmentRequest | undefined {
  const { site, subcontractor, driver, vehicle } = draft;
  const lines = draft.lines.map(toRecipeLine);
  const fireRate = parseFireRatePct(draft.fireRatePct ?? defaultFirePct);
  if (site === undefined || subcontractor === undefined || fireRate === undefined) return undefined;
  if (lines.length === 0 || lines.some((line) => line === undefined)) return undefined;
  return {
    siteId: site.value,
    subcontractorId: subcontractor.value,
    subcontractorTitle: subcontractor.label,
    lines: lines.filter((line): line is RecipeLineInput => line !== undefined),
    fireRate,
    ...(driver === undefined ? {} : { driverId: driver.value, driverName: driver.label }),
    ...(vehicle === undefined ? {} : { vehicleId: vehicle.value, plateNo: vehicle.label }),
  };
}
