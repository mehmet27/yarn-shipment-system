import { Alert, Stack } from '@iplik/ui';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { useRemoveBag } from '../hooks/useRemoveBag';
import { useShipmentBags } from '../hooks/useShipmentBags';
import { AssignBagsForm } from './AssignBagsForm';
import { BagSections } from './BagSections';
import { QueryState } from './QueryState';

/** Kör giriş: backend hangi listeyi vermediyse o bölüm hiç çizilmez (rol adı kontrolü yok). */
export function BagsTab({ shipmentId, canAssign }: { shipmentId: string; canAssign: boolean }) {
  const { t } = useTranslation();
  const query = useShipmentBags(shipmentId);
  const remove = useRemoveBag(shipmentId);
  return (
    <Stack>
      {canAssign && <AssignBagsForm shipmentId={shipmentId} />}
      {remove.isError && (
        <Alert color="red">
          {t(
            toProblem(remove.error).code === 'BAG_SERVICE_UNAVAILABLE'
              ? 'shipment.bags.assign.errors.BAG_SERVICE_UNAVAILABLE'
              : 'shipment.bags.removeFailed',
          )}
        </Alert>
      )}
      <QueryState query={query}>
        {(bags) => (
          <BagSections
            bags={bags}
            onRemove={
              canAssign
                ? (key) => {
                    remove.mutate(key);
                  }
                : undefined
            }
          />
        )}
      </QueryState>
    </Stack>
  );
}
