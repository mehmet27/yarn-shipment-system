import type { Permission } from '@iplik/api-client';
import type { BrandingSaveRequest, BrandingView } from '@iplik/api-client/branding';
import { DEFAULT_BRAND_COLOR } from '@iplik/ui';
import { http, HttpResponse } from 'msw';

const PATH = '/api/v1/branding';

let current: BrandingView = { primaryColor: DEFAULT_BRAND_COLOR };

/** Testler arasında sahte marka ayarını varsayılana döndürür. */
export function resetMockBranding(): void {
  current = { primaryColor: DEFAULT_BRAND_COLOR };
}

function isSaveRequest(value: unknown): value is BrandingSaveRequest {
  return (
    typeof value === 'object' &&
    value !== null &&
    'primaryColor' in value &&
    typeof value.primaryColor === 'string' &&
    /^#[0-9a-fA-F]{6}$/.test(value.primaryColor)
  );
}

/** Marka ayarı: okuma girişsiz, yazma yalnızca SETTINGS_MANAGE (yönetici), aksi halde 403. */
export function createBrandingHandlers(permissions: readonly Permission[]) {
  const canManage = permissions.includes('SETTINGS_MANAGE');
  return [
    http.get(PATH, () => HttpResponse.json(current)),
    http.put(PATH, async ({ request }) => {
      if (!canManage) {
        return HttpResponse.json(
          { status: 403, code: 'FORBIDDEN', title: 'Yetkiniz yok' },
          { status: 403 },
        );
      }
      const body: unknown = await request.json();
      if (!isSaveRequest(body)) {
        return HttpResponse.json(
          { status: 400, code: 'INVALID_MASTER_DATA', title: 'Geçersiz marka ayarı' },
          { status: 400 },
        );
      }
      current = {
        primaryColor: body.primaryColor.toLowerCase(),
        ...(body.companyName ? { companyName: body.companyName } : {}),
        ...(body.logoDataUrl ? { logoDataUrl: body.logoDataUrl } : {}),
      };
      return HttpResponse.json(current);
    }),
  ];
}
