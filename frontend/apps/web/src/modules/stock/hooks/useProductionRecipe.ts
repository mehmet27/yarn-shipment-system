import { listProducts } from '@iplik/api-client';
import { useState } from 'react';
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

/** Kumaş cinsi önerileri ve cinse tanımlı karışımı iplik satırlarına yazan işlem. */
export function useProductionRecipe(fabricType: string, setYarns: (rows: YarnLineDraft[]) => void) {
  const recipes = useActiveFabricRecipes().data ?? [];
  const recipe = findRecipe(recipes, fabricType);
  const [hasFailed, setHasFailed] = useState(false);
  const fill = () => {
    if (recipe === undefined) return;
    setHasFailed(false);
    Promise.all(recipe.yarns.map(toRow)).then(setYarns, () => {
      setHasFailed(true);
    });
  };
  return {
    fabricTypes: recipes.map((r) => r.fabricType),
    hasRecipe: recipe !== undefined,
    hasFailed,
    fill,
  };
}
