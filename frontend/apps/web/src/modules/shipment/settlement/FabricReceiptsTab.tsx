import { useTranslation } from '@iplik/i18n';
import { Stack, Text } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { QueryState } from '../components/QueryState';
import { FabricForm } from './FabricForm';
import { FabricReceiptCard } from './FabricReceiptCard';
import { useFabricReceipts } from './hooks';

/** Fasoncudan gelen kumaş girişleri; kayıt kalite/müdür/admin (FABRIC_RECEIPT_RECORD). */
export function FabricReceiptsTab({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const canRecord = usePermission('FABRIC_RECEIPT_RECORD');
  const query = useFabricReceipts(shipmentId);
  return (
    <Stack>
      {canRecord && <FabricForm shipmentId={shipmentId} />}
      <QueryState query={query}>
        {(receipts) =>
          receipts.length === 0 ? (
            <Text c="dimmed">{t('settlement.fabric.empty')}</Text>
          ) : (
            <Stack>
              {receipts.map((receipt) => (
                <FabricReceiptCard key={receipt.id} receipt={receipt} />
              ))}
            </Stack>
          )
        }
      </QueryState>
    </Stack>
  );
}
