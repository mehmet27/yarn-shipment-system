import type { FabricYarnOption } from '@iplik/api-client/fabric';
import { useEffect, useRef } from 'react';
import { findRecipe as findShipmentRecipe } from '../components/shipmentViews';
import { useShipment } from '../hooks/useShipment';
import { findRecipe, recipeToRatios } from './fabricRatios';
import { useActiveFabricRecipes } from './hooks';

type Fill = {
  fabricType: string;
  options: readonly FabricYarnOption[];
  setFabricType: (fabricType: string) => void;
  setRatios: (ratios: Record<string, string>) => void;
};

/** Sevkiyatın sipariş satırlarında tek bir kumaş cinsi yazılıysa o cins. */
function useOrderedFabricType(shipmentId: string): string | undefined {
  const shipment = useShipment(shipmentId).data;
  const lines = shipment === undefined ? [] : (findShipmentRecipe(shipment)?.lines ?? []);
  const types = [
    ...new Set(lines.map((line) => line.fabricType ?? '').filter((type) => type !== '')),
  ];
  return types.length === 1 ? types[0] : undefined;
}

/**
 * Kumaş cinsi seçilince fasoncudaki ipliklerin oranları tanımlı karışımdan dolar. Form açılınca sevkiyatta sipariş
 * edilen cins hazır seçilir. Karışımda olup fasoncuda bulunmayan iplikler ayrıca söylenir.
 */
export function useFabricRecipeFill(
  shipmentId: string,
  { fabricType, options, setFabricType, setRatios }: Fill,
) {
  const recipes = useActiveFabricRecipes().data ?? [];
  const change = (next: string) => {
    setFabricType(next);
    const recipe = findRecipe(recipes, next);
    setRatios(recipe === undefined ? {} : recipeToRatios(recipe, options));
  };
  const ordered = useOrderedFabricType(shipmentId);
  const isReady =
    ordered !== undefined && options.length > 0 && findRecipe(recipes, ordered) !== undefined;
  const isPrefilled = useRef(false);
  useEffect(() => {
    if (!isReady || isPrefilled.current) return;
    isPrefilled.current = true;
    if (fabricType === '') change(ordered);
  });
  const missing = (findRecipe(recipes, fabricType)?.yarns ?? [])
    .map((yarn) => yarn.yarnType)
    .filter((name) => !options.some((o) => o.yarnType.toLowerCase() === name.toLowerCase()));
  return { change, missingYarns: options.length === 0 ? [] : missing };
}
