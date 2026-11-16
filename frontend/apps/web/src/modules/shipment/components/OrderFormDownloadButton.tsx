import type { ShipmentDetail } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button } from '@iplik/ui';
import { shipmentOrderDraft } from '../create/shipmentOrderDraft';
import { useOrderFormPdf } from '../create/useOrderFormPdf';
import { findRecipe } from './shipmentViews';

/** Kayıtlı sevkiyatın sipariş formunu PDF olarak indirir; reçetesi görünmeyen görünümlerde (fasoncu) çıkmaz. */
export function OrderFormDownloadButton({ shipment }: { shipment: ShipmentDetail }) {
  const { t } = useTranslation();
  const downloadPdf = useOrderFormPdf();
  const recipe = findRecipe(shipment);
  if (recipe === undefined || recipe.lines.length === 0) return null;
  return (
    <Button
      variant="default"
      onClick={() => {
        void downloadPdf(shipmentOrderDraft(shipment.subcontractorTitle, recipe.lines));
      }}
    >
      {t('create.pdf.button')}
    </Button>
  );
}
