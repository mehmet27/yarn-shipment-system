import { useTranslation } from '@iplik/i18n';
import { Button, Modal } from '@iplik/ui';
import { useState } from 'react';
import { InvoiceEntry } from './InvoiceEntry';

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
        {isOpen && <InvoiceEntry shipmentId={shipmentId} onClose={close} />}
      </Modal>
    </>
  );
}
