import { useTranslation } from '@iplik/i18n';
import { Button, Icon } from '@iplik/ui';
import { useState } from 'react';
import { TransportEditor } from './TransportEditor';

export type UpdateShipmentTransportFormProps = {
  shipmentId: string;
  subcontractorId: string;
  driverName?: string | undefined;
  plateNo?: string | undefined;
};

/** Müdür: açık sevkiyatın şoför ve aracını düzeltir; önce "Şoför / araç değiştir" ile form açılır. */
export function UpdateShipmentTransportForm(props: UpdateShipmentTransportFormProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  if (isOpen) {
    return (
      <TransportEditor
        {...props}
        onClose={() => {
          setIsOpen(false);
        }}
      />
    );
  }
  return (
    <Button
      variant="light"
      w="fit-content"
      leftSection={<Icon name="truck" />}
      onClick={() => {
        setIsOpen(true);
      }}
    >
      {t('shipment.transport.open')}
    </Button>
  );
}
