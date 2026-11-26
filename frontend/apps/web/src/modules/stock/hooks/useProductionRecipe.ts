import { listProducts } from '@iplik/api-client';
import { useRef, useState } from 'react';
import { findRecipe, useActiveFabricRecipes } from '@/modules/shipment';
import type { YarnLineDraft } from '../stockDraft';

const SEARCH_PAGE_SIZE = 100;

const sameName = (a: string, b: string) => a.trim().toLowerCase() === b.trim().toLowerCase();

/** Karışımdaki iplik adının Tanımlar'daki aktif kaydı; pasife alınmışsa satır boş gelir, kullanıcı seçer. */
async function toRow(line: { yarnType: string; ratioPct: string }): Promise<YarnLineDraft> {
  const page = await listProducts({ q: line.yarnType, size: SEARCH_PAGE_SIZE });
  const match = page.items.find((item) => sameName(item.name, line.yarnType));
  return {
    product: match === undefined ? undefined : { value: match.id, label: match.name },
    ratio: String(Number(line.ratioPct)).replace('.', ','),
  };
}

type Setters = {
  setFabricType: (fabricType: string) => void;
  setYarns: (rows: YarnLineDraft[]) => void;
};

/**
 * Kumaş cinsi önerileri ve cins değişince iplik satırlarını değiştiren işlem: tanımlı cinse geçilince karışımı yazılır,
 * tanımlı cinsten çıkılınca satırlar boşalır. Karışımı olmayan cinste elle seçilen iplikler korunur.
 */
export function useProductionRecipe(fabricType: string, { setFabricType, setYarns }: Setters) {
  const recipes = useActiveFabricRecipes().data ?? [];
  const [hasFailed, setHasFailed] = useState(false);
  // Geç gelen iplik araması, sonradan seçilen cinsin satırlarını ezmesin.
  const latest = useRef(0);
  const change = (next: string) => {
    setFabricType(next);
    const before = findRecipe(recipes, fabricType);
    const recipe = findRecipe(recipes, next);
    if (before?.id === recipe?.id) return;
    latest.current += 1;
    const request = latest.current;
    setHasFailed(false);
    setYarns([{ product: undefined, ratio: '' }]);
    if (recipe === undefined) return;
    Promise.all(recipe.yarns.map(toRow)).then(
      (rows) => {
        if (request === latest.current) setYarns(rows);
      },
      () => {
        if (request === latest.current) setHasFailed(true);
      },
    );
  };
  return { fabricTypes: recipes.map((r) => r.fabricType), hasFailed, change };
}
