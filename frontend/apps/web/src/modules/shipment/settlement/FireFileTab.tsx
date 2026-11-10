import { useTranslation } from '@iplik/i18n';
import { Badge, Group, Paper, Stack, Title } from '@iplik/ui';
import { QueryState } from '../components/QueryState';
import { FireClosedNote } from './FireClosedNote';
import { FireFigures } from './FireFigures';
import { FireOpenActions } from './FireOpenActions';
import { useFireSettlement } from './hooks';

/** Fire dosyası kartı; işlemler izinle açılır, kapanmış dosya salt okunurdur. */
export function FireFileTab({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const query = useFireSettlement(shipmentId);
  return (
    <QueryState query={query}>
      {(settlement) => (
        <Paper withBorder p="md" component="section" aria-label={t('settlement.fire.title')}>
          <Stack>
            <Group justify="space-between">
              <Title order={2} size="h3">
                {t('settlement.fire.title')}
              </Title>
              <Badge data-testid="fire-status">
                {t(`settlement.fire.statuses.${settlement.status}`)}
              </Badge>
            </Group>
            <FireFigures settlement={settlement} />
            {settlement.status === 'AWAITING_FABRIC' || settlement.status === 'TO_INVOICE' ? (
              <FireOpenActions settlement={settlement} />
            ) : (
              <FireClosedNote settlement={settlement} />
            )}
          </Stack>
        </Paper>
      )}
    </QueryState>
  );
}
