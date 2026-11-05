import type { BrandingSaveRequest, BrandingView } from '@iplik/api-client/branding';
import { DEFAULT_BRAND_COLOR, isValidHexColor } from '@iplik/ui';

export const MAX_NAME_LENGTH = 80;
/** base64 şişmesiyle (×4/3) sunucunun 200 000 karakterlik sınırının altında kalır. */
export const MAX_LOGO_BYTES = 140_000;
export const LOGO_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/svg+xml'] as const;

/** Boş metin "kayıtlı değil" demektir (firma adı ve logo isteğe bağlıdır). */
export type BrandingDraft = { companyName: string; primaryColor: string; logoDataUrl: string };
export type BrandingError = 'color' | 'name';
export type LogoError = 'type' | 'size' | 'read';
export type LogoResult = { dataUrl: string } | { error: LogoError };

export const DEFAULT_DRAFT: BrandingDraft = {
  companyName: '',
  primaryColor: DEFAULT_BRAND_COLOR,
  logoDataUrl: '',
};

export function draftOf(branding: BrandingView): BrandingDraft {
  return {
    companyName: branding.companyName ?? '',
    primaryColor: branding.primaryColor,
    logoDataUrl: branding.logoDataUrl ?? '',
  };
}

export function validateBranding(draft: BrandingDraft): BrandingError | undefined {
  if (!isValidHexColor(draft.primaryColor)) {
    return 'color';
  }
  return draft.companyName.trim().length > MAX_NAME_LENGTH ? 'name' : undefined;
}

export function toRequest(draft: BrandingDraft): BrandingSaveRequest {
  const companyName = draft.companyName.trim();
  return {
    primaryColor: draft.primaryColor,
    ...(companyName === '' ? {} : { companyName }),
    ...(draft.logoDataUrl === '' ? {} : { logoDataUrl: draft.logoDataUrl }),
  };
}

function isLogoType(type: string): boolean {
  return LOGO_TYPES.some((allowed) => allowed === type);
}

function readAsDataUrl(file: File): Promise<LogoResult> {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = () => {
      resolve(typeof reader.result === 'string' ? { dataUrl: reader.result } : { error: 'read' });
    };
    reader.onerror = () => {
      resolve({ error: 'read' });
    };
    reader.readAsDataURL(file);
  });
}

/** Seçilen logoyu türü ve boyutu denetleyip base64 adrese çevirir. */
export function readLogo(file: File): Promise<LogoResult> {
  if (!isLogoType(file.type)) {
    return Promise.resolve({ error: 'type' });
  }
  if (file.size > MAX_LOGO_BYTES) {
    return Promise.resolve({ error: 'size' });
  }
  return readAsDataUrl(file);
}
