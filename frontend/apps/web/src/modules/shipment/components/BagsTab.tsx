import { useShipmentBags } from '../hooks/useShipmentBags';
import { BagSections } from './BagSections';
import { QueryState } from './QueryState';

/** Kör giriş: backend hangi listeyi vermediyse o bölüm hiç çizilmez (rol adı kontrolü yok). */
export function BagsTab({ shipmentId }: { shipmentId: string }) {
  const query = useShipmentBags(shipmentId);
  return <QueryState query={query}>{(bags) => <BagSections bags={bags} />}</QueryState>;
}
