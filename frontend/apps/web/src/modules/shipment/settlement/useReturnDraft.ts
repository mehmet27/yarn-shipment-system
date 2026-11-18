import { useState } from 'react';
import type { YarnReturnDraft } from './settlementDraft';
import { EMPTY_RETURN_DRAFT } from './settlementDraft';

export type ReturnField = 'netKg' | 'waybillNo';

export function useReturnDraft() {
  const [draft, setDraft] = useState<YarnReturnDraft>(EMPTY_RETURN_DRAFT);
  const field = (name: ReturnField) => ({
    value: draft[name],
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
      const { value } = event.currentTarget;
      setDraft((current) => ({ ...current, [name]: value }));
    },
  });
  const setYarnType = (yarnType: string) => {
    setDraft((current) => ({ ...current, yarnType }));
  };
  const setLotId = (lotId: string) => {
    setDraft((current) => ({ ...current, lotId }));
  };
  const reset = () => {
    setDraft(EMPTY_RETURN_DRAFT);
  };
  return { draft, field, setYarnType, setLotId, reset };
}
