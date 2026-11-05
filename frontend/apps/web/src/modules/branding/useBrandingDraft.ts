import type { BrandingView } from '@iplik/api-client/branding';
import { useState } from 'react';
import type { BrandingDraft, BrandingError } from './brandingDraft';
import { draftOf, toRequest, validateBranding } from './brandingDraft';
import { useSaveBranding } from './useSaveBranding';

/** Taslak alanlar, doğrulama ve kayıt; değişiklik "kaydedildi" bildirimini kaldırır. */
export function useBrandingDraft(branding: BrandingView) {
  const [draft, setDraft] = useState<BrandingDraft>(() => draftOf(branding));
  const [error, setError] = useState<BrandingError>();
  const [isSaved, setIsSaved] = useState(false);
  const mutation = useSaveBranding();
  const change = (next: Partial<BrandingDraft>) => {
    setDraft((current) => ({ ...current, ...next }));
    setIsSaved(false);
  };
  const submit = () => {
    const validation = validateBranding(draft);
    setError(validation);
    if (validation === undefined) {
      mutation.mutate(toRequest(draft), {
        onSuccess: () => {
          setIsSaved(true);
        },
      });
    }
  };
  return { draft, error, isSaved, mutation, change, submit };
}
