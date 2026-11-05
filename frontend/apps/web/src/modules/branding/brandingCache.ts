import type { BrandingView } from '@iplik/api-client/branding';
import { isValidHexColor } from '@iplik/ui';

const STORAGE_KEY = 'yarnshipment.branding';

function isStringOrUndefined(value: unknown): value is string | undefined {
  return value === undefined || typeof value === 'string';
}

function readOptionalText(
  value: object,
  key: 'companyName' | 'logoDataUrl',
): string | undefined | null {
  const field: unknown = key in value ? Reflect.get(value, key) : undefined;
  return isStringOrUndefined(field) ? field : null;
}

/** Depodan okunan değeri sözleşmeye göre daraltır; bozuk kayıt yok sayılır. */
function toBranding(value: unknown): BrandingView | undefined {
  if (typeof value !== 'object' || value === null || !('primaryColor' in value)) {
    return undefined;
  }
  const { primaryColor } = value;
  const companyName = readOptionalText(value, 'companyName');
  const logoDataUrl = readOptionalText(value, 'logoDataUrl');
  if (
    typeof primaryColor !== 'string' ||
    !isValidHexColor(primaryColor) ||
    companyName === null ||
    logoDataUrl === null
  ) {
    return undefined;
  }
  return {
    primaryColor,
    ...(companyName === undefined ? {} : { companyName }),
    ...(logoDataUrl === undefined ? {} : { logoDataUrl }),
  };
}

/**
 * Son bilinen marka ayarı: sayfa açılırken API yanıtı gelene kadar renk titremesin diye tarayıcıda tutulur.
 * Depo kapalı ya da bozuksa sessizce varsayılana düşülür.
 */
export function loadCachedBranding(): BrandingView | undefined {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw === null ? undefined : toBranding(JSON.parse(raw));
  } catch {
    return undefined;
  }
}

export function saveCachedBranding(branding: BrandingView): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(branding));
  } catch {
    // Depo dolu ya da kapalı: önbellek yalnızca kolaylıktır.
  }
}
