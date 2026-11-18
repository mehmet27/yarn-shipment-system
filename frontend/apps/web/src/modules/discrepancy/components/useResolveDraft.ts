import type { Resolution, ResolveDiscrepancyRequest } from '@iplik/api-client';
import { useState } from 'react';

/** Karar formunun taslağı: seçim ve not; gönderilebilir mi, hangi istek. */
export function useResolveDraft() {
  const [choice, setChoiceRaw] = useState<Resolution>();
  const [isConfirming, setConfirming] = useState(false);
  const [note, setNote] = useState('');
  const setChoice = (value: Resolution | undefined) => {
    setChoiceRaw(value);
    setConfirming(false);
  };
  const canSubmit = choice !== undefined && note.trim() !== '';
  const toRequest = (): ResolveDiscrepancyRequest | undefined =>
    choice === undefined ? undefined : { resolution: choice, note: note.trim() };
  return {
    ...{ choice, note, canSubmit, isConfirming },
    ...{ setChoice, setConfirming, setNote, toRequest },
  };
}
