import { useState } from 'react';
import type { MasterOption } from '../hooks/useMasterOptions';
import type { YarnReturnDraft } from './settlementDraft';
import { EMPTY_RETURN_DRAFT } from './settlementDraft';

export type ReturnField = 'yarnType' | 'netKg' | 'waybillNo';

export function useReturnDraft() {
  const [draft, setDraft] = useState<YarnReturnDraft>(EMPTY_RETURN_DRAFT);
  const setLot = (lot: MasterOption | undefined) => {
    setDraft((current) => ({ ...current, lot }));
  };
  const field = (name: ReturnField) => ({
    value: draft[name],
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
      const { value } = event.currentTarget;
      setDraft((current) => ({ ...current, [name]: value }));
    },
  });
  const reset = () => {
    setDraft(EMPTY_RETURN_DRAFT);
  };
  return { draft, field, setLot, reset };
}
