import { useTranslation } from '@iplik/i18n';
import { Box, Button, Icon } from '@iplik/ui';
import { useState } from 'react';
import { TransportEditor } from './TransportEditor';

export type UpdateShipmentTransportFormProps = {
  shipmentId: string;
  subcontractorId: string;
  driverName?: string | undefined;
  plateNo?: string | undefined;
};

/** Müdür: taslak sevkiyatın şoför ve aracını düzeltir; önce "Şoför / araç değiştir" ile form açılır. */
export function UpdateShipmentTransportForm(props: UpdateShipmentTransportFormProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  if (isOpen) {
    return (
      <Box w="100%">
        <TransportEditor
          {...props}
          onClose={() => {
            setIsOpen(false);
          }}
        />
      </Box>
    );
  }
  return (
    <Button
      variant="default"
      leftSection={<Icon name="truck" />}
      onClick={() => {
        setIsOpen(true);
      }}
    >
      {t('shipment.transport.open')}
    </Button>
  );
}
