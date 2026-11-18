import { useTranslation } from '@iplik/i18n';
import { Button, Stack } from '@iplik/ui';
import { InvoiceForm, QueryState, useFireSettlement } from '@/modules/shipment';
import { useState } from 'react';

/** Satırda "Fatura bilgisini gir" düğmesi; açılınca fatura no, tarih ve iplik başına kilo formu çıkar. */
export function InvoiceAction({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const [isOpen, setOpen] = useState(false);
  const query = useFireSettlement(shipmentId);
  return isOpen ? (
    <Stack mt="xs">
      <QueryState query={query}>
        {(settlement) => <InvoiceForm settlement={settlement} />}
      </QueryState>
      <Button
        variant="subtle"
        w="fit-content"
        onClick={() => {
          setOpen(false);
        }}
      >
        {t('settlement.toInvoice.cancelInvoice')}
      </Button>
    </Stack>
  ) : (
    <Button
      variant="light"
      w="fit-content"
      onClick={() => {
        setOpen(true);
      }}
    >
      {t('settlement.toInvoice.enterInvoice')}
    </Button>
  );
}
