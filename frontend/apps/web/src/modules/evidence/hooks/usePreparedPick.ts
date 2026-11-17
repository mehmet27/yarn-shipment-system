import { useRef, useState } from 'react';
import { preparePhoto } from '../preparePhoto';

/**
 * Seçilen dosyayı yüklemeye hazırlar (fotoğraf cihazda JPEG'e çevrilip küçültülür, PDF aynen geçer) ve `onPick`'e
 * verir; bitince girdiyi sıfırlar ki aynı dosya yeniden seçilince de change olayı gelsin.
 */
export function usePreparedPick(onPick: (file: File | null) => void) {
  const [isPreparing, setPreparing] = useState(false);
  const resetRef = useRef<() => void>(null);
  const pick = (file: File | null) => {
    if (file === null) return;
    setPreparing(true);
    void preparePhoto(file)
      .then(onPick)
      .finally(() => {
        setPreparing(false);
        resetRef.current?.();
      });
  };
  return { isPreparing, resetRef, pick };
}
