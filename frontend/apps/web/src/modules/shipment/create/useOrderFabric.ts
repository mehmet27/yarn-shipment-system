import { listProducts } from '@iplik/api-client';
import type { FabricRecipe } from '@iplik/api-client/fabric';
import { useRef } from 'react';
import { findRecipe } from '../settlement/fabricRatios';
import { useActiveFabricRecipes } from '../settlement/hooks';
import type { RecipeLineDraft } from './createDraft';

const SEARCH_PAGE_SIZE = 100;
const ELASTANE = /elastan|likra|lycra/i;

const sameName = (a: string, b: string) => a.trim().toLowerCase() === b.trim().toLowerCase();

/** Karışımdaki elastanın Tanımlar'daki numarası (ör. "30 denye"); elastan yoksa boş. */
async function lycraOf(recipe: FabricRecipe): Promise<string> {
  const elastane = recipe.yarns.find((yarn) => ELASTANE.test(yarn.yarnType));
  if (elastane === undefined) return '';
  const page = await listProducts({ q: elastane.yarnType, size: SEARCH_PAGE_SIZE });
  return page.items.find((item) => sameName(item.name, elastane.yarnType))?.yarnCount ?? '';
}

/**
 * Sipariş satırının kumaş cinsi: tanımlı karışımlar önerilir (yeni ad da yazılabilir). Tanımlı cinse geçilince LYC
 * karışımdaki elastandan dolar, tanımlı cinsten çıkılınca boşalır; seçili iplik karışımda yoksa uyarılır.
 */
export function useOrderFabric(
  line: RecipeLineDraft,
  onPatch: (patch: Partial<RecipeLineDraft>) => void,
) {
  const recipes = useActiveFabricRecipes().data ?? [];
  // Elastan araması bittiğinde satırın güncel hali yazılsın; geç gelen cevap yeni seçimi ezmesin.
  const patch = useRef(onPatch);
  patch.current = onPatch;
  const latest = useRef(0);
  const change = (fabricType: string) => {
    onPatch({ fabricType });
    const before = findRecipe(recipes, line.fabricType);
    const recipe = findRecipe(recipes, fabricType);
    if (before?.id === recipe?.id) return;
    latest.current += 1;
    const request = latest.current;
    onPatch({ fabricType, lycra: '' });
    if (recipe === undefined) return;
    // Arama başarısız olursa LYC boş kalır ve elle yazılır.
    lycraOf(recipe).then(
      (lycra) => {
        if (request === latest.current && lycra !== '') patch.current({ lycra });
      },
      () => undefined,
    );
  };
  const yarn = line.yarn?.label;
  const recipe = findRecipe(recipes, line.fabricType);
  return {
    fabricTypes: recipes.map((r) => r.fabricType),
    change,
    isYarnOutsideRecipe:
      yarn !== undefined &&
      recipe !== undefined &&
      !recipe.yarns.some((item) => sameName(item.yarnType, yarn)),
  };
}
