import { useState } from 'react';

/** Onay adımı: yalnız onaylanan isteğin kendisi için açık kalır; taslak değişince yeniden sorulur. */
export function useConfirmStep(request: unknown) {
  const [asked, setAsked] = useState<string>();
  const key = JSON.stringify(request);
  return {
    isOpen: asked !== undefined && asked === key,
    ask: () => {
      setAsked(key);
    },
    close: () => {
      setAsked(undefined);
    },
  };
}
