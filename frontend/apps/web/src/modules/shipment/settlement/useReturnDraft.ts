import { useState } from 'react';
import type { YarnReturnDraft } from './settlementDraft';
import { EMPTY_RETURN_DRAFT } from './settlementDraft';

export type ReturnField = 'yarnType' | 'lotId' | 'netKg' | 'waybillNo';

export function useReturnDraft() {
  const [draft, setDraft] = useState<YarnReturnDraft>(EMPTY_RETURN_DRAFT);
  const field = (name: ReturnField) => ({
    value: draft[name],
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
      const { value } = event.currentTarget;
      setDraft((current) => ({ ...current, [name]: value }));
    },
  });
  const setDirection = (direction: YarnReturnDraft['direction']) => {
    setDraft((current) => ({ ...current, direction }));
  };
  const reset = () => {
    setDraft(EMPTY_RETURN_DRAFT);
  };
  return { draft, field, setDirection, reset };
}
