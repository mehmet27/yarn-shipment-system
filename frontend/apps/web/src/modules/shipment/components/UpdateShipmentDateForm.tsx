import { useTranslation } from '@iplik/i18n';
import { Box, Button, Icon } from '@iplik/ui';
import { useState } from 'react';
import { UpdateShipmentDateEditor } from './UpdateShipmentDateEditor';

export type UpdateShipmentDateFormProps = { shipmentId: string; shipmentDate: string };

/** Müdür: taslak sevkiyatın gününü değiştirir; önce "Tarihi değiştir" ile form açılır. */
export function UpdateShipmentDateForm({ shipmentId, shipmentDate }: UpdateShipmentDateFormProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  if (!isOpen) {
    return (
      <Button
        variant="default"
        leftSection={<Icon name="calendar" />}
        onClick={() => {
          setIsOpen(true);
        }}
      >
        {t('shipment.schedule.open')}
      </Button>
    );
  }
  return (
    <Box w="100%">
      <UpdateShipmentDateEditor
        shipmentId={shipmentId}
        shipmentDate={shipmentDate}
        onClose={() => {
          setIsOpen(false);
        }}
      />
    </Box>
  );
}
