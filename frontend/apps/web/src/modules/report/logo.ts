/** Rapor logosu bu tarayıcıda saklanır; kişi ister eklesin, PDF çıktısının başında görünür. */
const STORAGE_KEY = 'iplik.report.logo';
export const MAX_LOGO_BYTES = 204_800;
export const LOGO_TYPES = ['image/png', 'image/jpeg'] as const;

export function isLogoType(type: string): boolean {
  return LOGO_TYPES.some((allowed) => allowed === type);
}

export function loadLogo(): string | null {
  try {
    const value = window.localStorage.getItem(STORAGE_KEY);
    return value?.startsWith('data:image/') === true ? value : null;
  } catch {
    return null;
  }
}

/** Saklanamazsa (özel pencere, kota) false döner; ekran yine de bu oturumda logoyu kullanır. */
export function saveLogo(dataUrl: string | null): boolean {
  try {
    if (dataUrl === null) window.localStorage.removeItem(STORAGE_KEY);
    else window.localStorage.setItem(STORAGE_KEY, dataUrl);
    return true;
  } catch {
    return false;
  }
}

export function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') resolve(reader.result);
      else reject(new Error('Logo okunamadı'));
    };
    reader.onerror = () => {
      reject(new Error('Logo okunamadı'));
    };
    reader.readAsDataURL(file);
  });
}
