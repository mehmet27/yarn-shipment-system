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
  /** Sipariş formu alanları; boş metin "girilmedi" demektir. */
  fabricType: string;
  lycra: string;
  /** `''`, `TUBE`, `MAY` ya da `OPEN_WIDTH`. */
  widthType: string;
  yarnLength: string;
  pussFine: string;
  /** TL/kg, Türkçe ondalık virgülü kabul edilir. */
  unitPrice: string;
};

/** Seçilen kayıtlar kimlik ve görünen ad birlikte tutulur; istek ikisini de ister. */
export type CreateDraft = {
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
    fabricType: '',
    lycra: '',
    widthType: '',
    yarnLength: '',
    pussFine: '',
    unitPrice: '',
  };
}

export const EMPTY_CREATE_DRAFT: CreateDraft = {
  subcontractor: undefined,
  driver: undefined,
  vehicle: undefined,
  lines: [newLine()],
  fireRatePct: undefined,
};

/** i18n anahtarı (`create.errors.*`). */
export type CreateError =
  | 'subcontractorRequired'
  | 'linesRequired'
  | 'lineYarnRequired'
  | 'lineKgInvalid'
  | 'linePriceInvalid'
  | 'fireRateInvalid';

export type CreateFieldErrors = Partial<
  Record<'subcontractor' | 'lines' | 'fireRate', CreateError>
>;

const POSITIVE_KG = /[1-9]/;

/** Net kg geçerli ve 0'dan büyük olmalı (3 ondalık). */
export function parsePositiveKg(input: string): Kilogram | undefined {
  const kg = parseKg(input);
  return kg !== undefined && POSITIVE_KG.test(kg) ? kg : undefined;
}

const PRICE = /^\d{1,8}([.,]\d{1,4})?$/;

/** Boş fiyat geçerlidir (isteğe bağlı); doluysa en çok 4 ondalıklı, negatif olmayan sayı olmalı. */
export function priceValid(input: string): boolean {
  const text = input.trim();
  return text === '' || PRICE.test(text);
}

export function lineError(line: RecipeLineDraft): CreateError | undefined {
  if (line.yarn === undefined && line.yarnText.trim() === '') return 'lineYarnRequired';
  if (parsePositiveKg(line.netKg) === undefined) return 'lineKgInvalid';
  return priceValid(line.unitPrice) ? undefined : 'linePriceInvalid';
}

/** Tüm eksik zorunlu alanlar birden döner; hata mesajı ilgili alanın altında gösterilir. */
export function validateCreateFields(draft: CreateDraft, defaultFirePct = '4'): CreateFieldErrors {
  const firstLineError = draft.lines.map(lineError).find((error) => error !== undefined);
  return {
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
  return errors.subcontractor ?? errors.lines ?? errors.fireRate;
}

/** Fasoncu değişince önceki fasoncunun şoförü seçili kalmaz. */
export function withSubcontractor(draft: CreateDraft, subcontractor: MasterOption | undefined) {
  return { ...draft, subcontractor, driver: undefined };
}

/** Geçerli satırların net toplamı; geçersiz satırlar sayılmaz. */
export function totalNetKg(lines: readonly RecipeLineDraft[]): string {
  return sumKg(lines.map((line) => parsePositiveKg(line.netKg) ?? ''));
}

/** Boş metin istekte hiç yer almaz. */
function filled(value: string): string | undefined {
  const text = value.trim();
  return text === '' ? undefined : text;
}

function orderFormFields(line: RecipeLineDraft): Partial<RecipeLineInput> {
  const fabricType = filled(line.fabricType);
  const lycra = filled(line.lycra);
  const widthType = filled(line.widthType);
  const yarnLength = filled(line.yarnLength);
  const pussFine = filled(line.pussFine);
  const unitPrice = filled(line.unitPrice.replace(',', '.'));
  return {
    ...(fabricType === undefined ? {} : { fabricType }),
    ...(lycra === undefined ? {} : { lycra }),
    ...(widthType === undefined ? {} : { widthType }),
    ...(yarnLength === undefined ? {} : { yarnLength }),
    ...(pussFine === undefined ? {} : { pussFine }),
    ...(unitPrice === undefined ? {} : { unitPrice }),
  };
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
    ...orderFormFields(line),
  };
}

/** Lokasyon firmanın tek lokasyonudur, formda seçilmez. Boş bırakılan şoför ve araç istekte hiç yer almaz; fire oranı kesirdir (%4 → 0.04). */
export function toCreateRequest(
  draft: CreateDraft,
  siteId: string,
  defaultFirePct = '4',
): CreateShipmentRequest | undefined {
  const { subcontractor, driver, vehicle } = draft;
  const lines = draft.lines.map(toRecipeLine);
  const fireRate = parseFireRatePct(draft.fireRatePct ?? defaultFirePct);
  if (subcontractor === undefined || fireRate === undefined) return undefined;
  if (lines.length === 0 || lines.some((line) => line === undefined)) return undefined;
  return {
    siteId,
    subcontractorId: subcontractor.value,
    subcontractorTitle: subcontractor.label,
    lines: lines.filter((line): line is RecipeLineInput => line !== undefined),
    fireRate,
    ...(driver === undefined ? {} : { driverId: driver.value, driverName: driver.label }),
    ...(vehicle === undefined ? {} : { vehicleId: vehicle.value, plateNo: vehicle.label }),
  };
}
