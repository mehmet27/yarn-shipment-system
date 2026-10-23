import { Stack } from '@iplik/ui';
import { useShipmentBags } from '../hooks/useShipmentBags';
import { AssignBagsForm } from './AssignBagsForm';
import { BagSections } from './BagSections';
import { QueryState } from './QueryState';

/** Kör giriş: backend hangi listeyi vermediyse o bölüm hiç çizilmez (rol adı kontrolü yok). */
export function BagsTab({ shipmentId, canAssign }: { shipmentId: string; canAssign: boolean }) {
  const query = useShipmentBags(shipmentId);
  return (
    <Stack>
      {canAssign && <AssignBagsForm shipmentId={shipmentId} />}
      <QueryState query={query}>{(bags) => <BagSections bags={bags} />}</QueryState>
    </Stack>
  );
}
