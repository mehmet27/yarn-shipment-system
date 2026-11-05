import type { DeliveryScanView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Paper, Text } from '@iplik/ui';
import { formatDateTime } from './formatDateTime';

/** Teslimde okutulan tek anahtar: tür, zaman ve (yalnızca iç görünümde) eşleşme sonucu. */
export function DeliveryScanRow({ scan }: { scan: DeliveryScanView }) {
  const { t } = useTranslation();
  return (
    <Paper withBorder p="sm">
      <Text fw={600}>{scan.scannedKey}</Text>
      <Text size="sm" c="dimmed">
        {scan.packageType === undefined ? '' : `${t(`bag.packageTypes.${scan.packageType}`)} · `}
        {formatDateTime(scan.scannedAt)}
      </Text>
      {scan.matched !== undefined && (
        <Badge color={scan.matched ? 'green' : 'red'}>
          {t(scan.matched ? 'shipment.bags.matched' : 'shipment.bags.unmatched')}
        </Badge>
      )}
    </Paper>
  );
}
