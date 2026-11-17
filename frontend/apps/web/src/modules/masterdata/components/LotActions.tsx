import type { LotView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, Group, Icon, Stack } from '@iplik/ui';
import { useState } from 'react';
import { useSetLotActive } from '../hooks/useLotMutations';
import { DeactivateLotConfirm } from './DeactivateLotConfirm';

export type LotActionsProps = { lot: LotView; isLocked: boolean; onEdit: (lot: LotView) => void };

/** Düzenle (kilitsizde) ve pasife al / yeniden aç; pasife alma sessiz bir düğmedir ve onay ister. */
export function LotActions({ lot, isLocked, onEdit }: LotActionsProps) {
  const { t } = useTranslation();
  const mutation = useSetLotActive();
  const [isConfirmingOff, setConfirmingOff] = useState(false);
  return (
    <Stack gap="sm">
      <Group grow>
        {!isLocked && (
          <Button
            variant="default"
            leftSection={<Icon name="edit" size={16} />}
            onClick={() => {
              onEdit(lot);
            }}
          >
            {t('masterData.edit')}
          </Button>
        )}
        <Button
          variant={lot.active ? 'subtle' : 'light'}
          color={lot.active ? 'gray' : 'green'}
          loading={mutation.isPending}
          leftSection={<Icon name={lot.active ? 'stop' : 'check'} size={16} />}
          onClick={() => {
            if (lot.active) setConfirmingOff(true);
            else mutation.mutate({ id: lot.id, isActive: true });
          }}
        >
          {t(lot.active ? 'masterData.deactivate' : 'masterData.activate')}
        </Button>
      </Group>
      {isConfirmingOff && lot.active && (
        <DeactivateLotConfirm
          lotNo={lot.lotNo}
          isLoading={mutation.isPending}
          onConfirm={() => {
            mutation.mutate({ id: lot.id, isActive: false });
            setConfirmingOff(false);
          }}
          onCancel={() => {
            setConfirmingOff(false);
          }}
        />
      )}
    </Stack>
  );
}
