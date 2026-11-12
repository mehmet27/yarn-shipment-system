import type { ShipmentDetail } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Stack } from '@iplik/ui';
import { EvidenceGallery } from '@/modules/evidence';
import { DispatchWeighingPanel } from './DispatchWeighingPanel';
import { RecipePanel } from './RecipePanel';
import { findDispatchWeighing, findRecipe } from './shipmentViews';

/** Kilolar sekmesi: çıkış tartısı ve (görünümde varsa) reçete ile fire oranı. */
export function WeightsPanel({ shipment }: { shipment: ShipmentDetail }) {
  const { t } = useTranslation();
  const recipe = findRecipe(shipment);
  return (
    <Stack gap="lg">
      <DispatchWeighingPanel weighing={findDispatchWeighing(shipment)} />
      <EvidenceGallery
        shipmentId={shipment.shipmentId}
        types={['DISPATCH_SCALE']}
        title={t('evidence.view.scaleTicket')}
        hideWhenEmpty
      />
      {recipe !== undefined && <RecipePanel lines={recipe.lines} fireRate={recipe.fireRate} />}
    </Stack>
  );
}
