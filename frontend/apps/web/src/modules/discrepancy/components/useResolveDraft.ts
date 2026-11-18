import type { Resolution } from '@iplik/api-client';
import { useState } from 'react';
import type { ResolveDiscrepancyRequest } from '@iplik/api-client';

/** Karar formunun taslağı: seçim, not ve sorumlu taraf; gönderilebilir mi, hangi istek. */
export function useResolveDraft() {
  const [choice, setChoiceRaw] = useState<Resolution>();
  const [isConfirming, setConfirming] = useState(false);
  const [note, setNote] = useState('');
  const [liableParty, setLiableParty] = useState('');
  const setChoice = (value: Resolution | undefined) => {
    setChoiceRaw(value);
    setConfirming(false);
  };
  const isPartyRequired = choice === 'LIABILITY_ASSIGNED';
  const canSubmit =
    choice !== undefined && note.trim() !== '' && (!isPartyRequired || liableParty.trim() !== '');
  const toRequest = (): ResolveDiscrepancyRequest | undefined => {
    if (choice === undefined) return undefined;
    return {
      resolution: choice,
      note: note.trim(),
      ...(isPartyRequired ? { liableParty: liableParty.trim() } : {}),
    };
  };
  return {
    ...{ choice, note, liableParty, isPartyRequired, canSubmit, isConfirming },
    ...{ setChoice, setConfirming, setNote, setLiableParty, toRequest },
  };
}
