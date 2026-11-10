import type { FabricDraft, YarnRowDraft } from './settlementDraft';
import { EMPTY_FABRIC_DRAFT } from './settlementDraft';
import { useState } from 'react';

export type FabricField = 'fabricType' | 'netKg' | 'rollCount' | 'waybillNo';

/** Kumaş girişi taslağı: alan ve iplik satırı düzenleme işlemleri. */
export function useFabricDraft() {
  const [draft, setDraft] = useState<FabricDraft>(EMPTY_FABRIC_DRAFT);
  const field = (name: FabricField) => ({
    value: draft[name],
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
      const { value } = event.currentTarget;
      setDraft((current) => ({ ...current, [name]: value }));
    },
  });
  const setYarn = (index: number, patch: Partial<YarnRowDraft>) => {
    setDraft((current) => ({
      ...current,
      yarns: current.yarns.map((row, i) => (i === index ? { ...row, ...patch } : row)),
    }));
  };
  const addYarn = () => {
    setDraft((current) => ({ ...current, yarns: [...current.yarns, { yarnType: '', netKg: '' }] }));
  };
  const removeYarn = (index: number) => {
    setDraft((current) => ({ ...current, yarns: current.yarns.filter((_, i) => i !== index) }));
  };
  const reset = () => {
    setDraft(EMPTY_FABRIC_DRAFT);
  };
  return { draft, field, setYarn, addYarn, removeYarn, reset };
}
