import { useEffect, useState } from 'react';

const FLASH_MS = 4000;

/** Kısa süreli başarı iletisi: `show()` çağrılınca birkaç saniye `isVisible` olur. */
export function useFlash() {
  const [isVisible, setIsVisible] = useState(false);
  useEffect(() => {
    if (!isVisible) return undefined;
    const timer = setTimeout(() => {
      setIsVisible(false);
    }, FLASH_MS);
    return () => {
      clearTimeout(timer);
    };
  }, [isVisible]);
  return {
    isVisible,
    show: () => {
      setIsVisible(true);
    },
  };
}
