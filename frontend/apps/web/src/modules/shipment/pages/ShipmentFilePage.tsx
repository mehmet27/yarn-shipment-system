import { Stack } from '@iplik/ui';
import { useParams } from 'react-router';
import { QueryState } from '@/modules/shared';
import { ShipmentFileTabs } from '../components/ShipmentFileTabs';
import { ShipmentFileTop } from '../components/ShipmentFileTop';
import { useShipment } from '../hooks/useShipment';

/** Mimari doküman "Sevkiyat dosyası ekranı": Yönetici ve Kalite için merkez ekran. */
export function ShipmentFilePage() {
  const { shipmentId = '' } = useParams();
  const query = useShipment(shipmentId);
  return (
    <QueryState query={query}>
      {(shipment) => (
        <Stack>
          <ShipmentFileTop shipment={shipment} />
          <ShipmentFileTabs shipment={shipment} />
        </Stack>
      )}
    </QueryState>
  );
}
