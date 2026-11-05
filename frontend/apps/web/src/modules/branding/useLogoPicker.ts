import { useState } from 'react';
import type { LogoError } from './brandingDraft';
import { readLogo } from './brandingDraft';

/** Seçilen dosyayı okur; hatayı saklar, başarılıysa adresi `onPicked`'a verir. */
export function useLogoPicker(onPicked: (dataUrl: string) => void) {
  const [error, setError] = useState<LogoError>();
  const pick = async (file: File | null) => {
    if (file === null) {
      return;
    }
    const result = await readLogo(file);
    if ('error' in result) {
      setError(result.error);
      return;
    }
    setError(undefined);
    onPicked(result.dataUrl);
  };
  return {
    error,
    pick,
    clearError: () => {
      setError(undefined);
    },
  };
}
