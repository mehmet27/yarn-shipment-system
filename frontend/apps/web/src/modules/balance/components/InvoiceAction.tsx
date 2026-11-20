import { useTranslation } from '@iplik/i18n';
import { Button, Divider, Modal, Stack } from '@iplik/ui';
import { InvoiceForm, useFireSettlement } from '@/modules/shipment';
import { QueryState } from '@/modules/shared';
import { useState } from 'react';
import { PreviousInvoices } from './PreviousInvoices';

/** Satırda "Fatura bilgisini gir" düğmesi; açılan pencerede önceki faturalar ve yeni fatura formu çıkar. */
export function InvoiceAction({
  shipmentId,
  shipmentNo,
}: {
  shipmentId: string;
  shipmentNo: string;
}) {
  const { t } = useTranslation();
  const [isOpen, setOpen] = useState(false);
  const query = useFireSettlement(shipmentId);
  const close = () => {
    setOpen(false);
  };
  return (
    <>
      <Button
        variant="light"
        w="fit-content"
        onClick={() => {
          setOpen(true);
        }}
      >
        {t('settlement.toInvoice.enterInvoice')}
      </Button>
      <Modal
        opened={isOpen}
        onClose={close}
        title={t('settlement.toInvoice.modalTitle', { no: shipmentNo })}
      >
        <QueryState query={query}>
          {(settlement) => (
            <Stack>
              <PreviousInvoices settlement={settlement} />
              <Divider />
              <InvoiceForm settlement={settlement} onSaved={close} />
              <Button variant="subtle" w="fit-content" onClick={close}>
                {t('settlement.toInvoice.cancelInvoice')}
              </Button>
            </Stack>
          )}
        </QueryState>
      </Modal>
    </>
  );
}
