import { useState } from 'react';
import type { ProductionDraft, YarnLineDraft } from './stockDraft';
import { emptyProductionDraft } from './stockDraft';

export type ProductionTextField = 'fabricType' | 'firePct' | 'rollCount' | 'waybillNo';

/** Merkez kumaş girişi taslağı: alan ve iplik satırı düzenleme işlemleri. */
export function useProductionDraft(defaultFirePct: string) {
  const [draft, setDraft] = useState<ProductionDraft>(() => emptyProductionDraft(defaultFirePct));
  const field = (name: ProductionTextField) => ({
    value: draft[name],
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
      const { value } = event.currentTarget;
      setDraft((current) => ({ ...current, [name]: value }));
    },
  });
  const setYarn = (index: number, patch: Partial<YarnLineDraft>) => {
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
    setDraft(emptyProductionDraft(defaultFirePct));
  };
  return { draft, field, setYarn, addYarn, removeYarn, reset };
}
