import { useTranslation } from '@iplik/i18n';
import { Button, Divider, Stack } from '@iplik/ui';
import { InvoiceForm, QueryState, useFireSettlement } from '@/modules/shipment';
import { PreviousInvoices } from './PreviousInvoices';

/** Fire dosyası yalnızca pencere açılınca okunur; listedeki her satır için ayrı istek gitmez. */
export function InvoiceEntry({ shipmentId, onClose }: { shipmentId: string; onClose: () => void }) {
  const { t } = useTranslation();
  const query = useFireSettlement(shipmentId);
  return (
    <QueryState query={query}>
      {(settlement) => (
        <Stack>
          <PreviousInvoices settlement={settlement} />
          <Divider />
          <InvoiceForm settlement={settlement} onSaved={onClose} />
          <Button variant="subtle" w="fit-content" onClick={onClose}>
            {t('settlement.toInvoice.cancelInvoice')}
          </Button>
        </Stack>
      )}
    </QueryState>
  );
}
