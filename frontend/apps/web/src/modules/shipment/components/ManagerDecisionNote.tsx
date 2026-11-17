import type { ShipmentDetail } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Stack, Text } from '@iplik/ui';

/**
 * Kantar uyuşmazlığında müdür "çuval ekle-çıkar" dediyse sevkiyat taslağa döner; depo bu notu görüp yalnızca istenen
 * işlemi yapar. Başka durumda ya da başka kararda hiçbir şey çizmez.
 */
export function ManagerDecisionNote({ shipment }: { shipment: ShipmentDetail }) {
  const { t } = useTranslation();
  const check = shipment.weightCheck;
  if (shipment.status !== 'DRAFT' || check?.decision !== 'ADD_REMOVE_BAGS') return null;
  return (
    <Alert
      color="orange"
      title={t('shipment.managerNote.title')}
      data-testid="manager-decision-note"
    >
      <Stack gap={4}>
        <Text size="sm">{t('shipment.managerNote.action')}</Text>
        {check.decisionReason !== undefined && (
          <Text size="sm">
            {t('shipment.managerNote.reason')}: {check.decisionReason}
          </Text>
        )}
      </Stack>
    </Alert>
  );
}
