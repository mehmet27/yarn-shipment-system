import { Alert, Stack } from '@iplik/ui';
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
      {remove.isError && <Alert color="red">{t('shipment.bags.removeFailed')}</Alert>}
      <QueryState query={query}>
        {(bags) => (
          <>
            {canAssign && (
              <AssignBagsForm shipmentId={shipmentId} assignedCount={bags.dispatchBags.length} />
            )}
            <BagSections
              bags={bags}
              onRemove={
                canAssign
                  ? (seqNo) => {
                      remove.mutate(seqNo);
                    }
                  : undefined
              }
            />
          </>
        )}
      </QueryState>
    </Stack>
  );
}
