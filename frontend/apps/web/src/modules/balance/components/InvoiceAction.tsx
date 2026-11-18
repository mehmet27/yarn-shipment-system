import { useTranslation } from '@iplik/i18n';
import { Button, Stack } from '@iplik/ui';
import { InvoiceForm } from '@/modules/shipment';
import { useState } from 'react';

/** Satırda "Fatura bilgisini gir" düğmesi; açılınca fatura no / tarih formu çıkar (fatura ERP'de kesilmiştir). */
export function InvoiceAction({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const [isOpen, setOpen] = useState(false);
  if (isOpen) {
    return (
      <Stack mt="xs">
        <InvoiceForm shipmentId={shipmentId} />
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
    );
  }
  return (
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
