import { useState } from 'react';
import type { FabricDraft } from './settlementDraft';
import { EMPTY_FABRIC_DRAFT } from './settlementDraft';

export type FabricField = 'fabricType' | 'netKg' | 'rollCount' | 'waybillNo';

/** Kumaş girişi taslağı: alanlar ve iplik satırlarına yazılan oranlar. */
export function useFabricDraft() {
  const [draft, setDraft] = useState<FabricDraft>(EMPTY_FABRIC_DRAFT);
  const field = (name: FabricField) => ({
    value: draft[name],
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
      const { value } = event.currentTarget;
      setDraft((current) => ({ ...current, [name]: value }));
    },
  });
  const setRatio = (key: string, value: string) => {
    setDraft((current) => ({ ...current, ratios: { ...current.ratios, [key]: value } }));
  };
  const setRatios = (ratios: Record<string, string>) => {
    setDraft((current) => ({ ...current, ratios }));
  };
  const reset = () => {
    setDraft(EMPTY_FABRIC_DRAFT);
  };
  return { draft, field, setRatio, setRatios, reset };
}
