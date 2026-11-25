import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, SectionCard, Stack, Text } from '@iplik/ui';
import { QueryState } from '@/modules/shared';
import { FireClosedNote } from './FireClosedNote';
import { FireFigures } from './FireFigures';
import { FireOpenActions } from './FireOpenActions';
import { useFireSettlement } from './hooks';
import { YarnUsedForFabric } from './YarnUsedForFabric';

/** Fire dosyası kartı; işlemler izinle açılır, kapanmış dosya salt okunurdur. */
export function FireFileTab({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const query = useFireSettlement(shipmentId);
  if (query.isError && toProblem(query.error).code === 'FIRE_SETTLEMENT_NOT_FOUND') {
    return <Text c="dimmed">{t('settlement.fire.notOpened')}</Text>;
  }
  return (
    <QueryState query={query}>
      {(settlement) => (
        <Stack>
          <SectionCard
            title={t('settlement.fire.title')}
            actions={
              <Badge variant="light" data-testid="fire-status" style={{ flexShrink: 0 }}>
                {t(`settlement.fire.statuses.${settlement.status}`)}
              </Badge>
            }
          >
            <Stack>
              <FireFigures settlement={settlement} />
              {settlement.status === 'AWAITING_FABRIC' || settlement.status === 'TO_INVOICE' ? (
                <FireOpenActions settlement={settlement} />
              ) : (
                <FireClosedNote settlement={settlement} />
              )}
            </Stack>
          </SectionCard>
          <YarnUsedForFabric shipmentId={shipmentId} />
        </Stack>
      )}
    </QueryState>
  );
}
