import type { LotView } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Paper, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { useLockLot } from '../hooks/useLotMutations';

/** Kilit geri alınamaz: önce "Kaliteyi kilitle", sonra parti ve kaliteyi gösteren onay. */
export function LockLotButton({ lot }: { lot: LotView }) {
  const { t } = useTranslation();
  const [isConfirming, setConfirming] = useState(false);
  const mutation = useLockLot();
  if (!isConfirming) {
    return (
      <Button
        color="orange"
        variant="light"
        onClick={() => {
          setConfirming(true);
        }}
      >
        {t('lots.lock.open')}
      </Button>
    );
  }
  const code = mutation.isError ? toProblem(mutation.error).code : undefined;
  return (
    <Paper withBorder p="sm">
      <Stack gap="sm">
        <Text size="sm">
          {t('lots.lock.question', { lotNo: lot.lotNo, grade: lot.qualityGradeCode })}
        </Text>
        {code !== undefined && (
          <Alert color="red">
            {t(
              code === 'LOT_QUALITY_LOCKED'
                ? 'lots.serverErrors.LOT_QUALITY_LOCKED'
                : 'lots.lock.failed',
            )}
          </Alert>
        )}
        <Group grow>
          <Button
            color="orange"
            loading={mutation.isPending}
            onClick={() => {
              mutation.mutate(lot.id);
            }}
          >
            {t('lots.lock.confirm')}
          </Button>
          <Button
            variant="default"
            onClick={() => {
              setConfirming(false);
              mutation.reset();
            }}
          >
            {t('lots.lock.dismiss')}
          </Button>
        </Group>
      </Stack>
    </Paper>
  );
}
