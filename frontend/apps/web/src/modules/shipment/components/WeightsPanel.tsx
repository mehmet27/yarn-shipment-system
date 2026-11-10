import type { ShipmentDetail } from '@iplik/api-client';
import { Stack } from '@iplik/ui';
import { DispatchWeighingPanel } from './DispatchWeighingPanel';
import { RecipePanel } from './RecipePanel';
import { findDispatchWeighing, findRecipe } from './shipmentViews';

/** Kilolar sekmesi: çıkış tartısı ve (görünümde varsa) reçete ile fire oranı. */
export function WeightsPanel({ shipment }: { shipment: ShipmentDetail }) {
  const recipe = findRecipe(shipment);
  return (
    <Stack gap="lg">
      <DispatchWeighingPanel weighing={findDispatchWeighing(shipment)} />
      {recipe !== undefined && <RecipePanel lines={recipe.lines} fireRate={recipe.fireRate} />}
    </Stack>
  );
}
