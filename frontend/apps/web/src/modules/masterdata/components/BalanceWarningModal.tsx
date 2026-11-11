import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Modal, Stack, Text } from '@iplik/ui';
import { formatKgText } from '@iplik/units';
import type { PendingWarning } from '../hooks/useDeactivateGuard';

export type BalanceWarningModalProps = {
  warning: PendingWarning | undefined;
  onConfirm: () => void;
  onClose: () => void;
};

/** KR-18: bakiyesi olan fasoncu pasife alınırken uyarır; karar yöneticidedir. */
export function BalanceWarningModal({ warning, onConfirm, onClose }: BalanceWarningModalProps) {
  const { t } = useTranslation();
  const title = warning?.record.label ?? '';
  return (
    <Modal
      opened={warning !== undefined}
      onClose={onClose}
      title={t('masterData.balanceWarning.title')}
    >
      <Stack>
        <Alert color="orange" role="alert">
          {t(
            warning?.rows === undefined
              ? 'masterData.balanceWarning.unknown'
              : 'masterData.balanceWarning.message',
            { title },
          )}
        </Alert>
        {warning?.rows?.map((row) => (
          <Text key={row.yarnType} size="sm">
            {row.yarnType}:{' '}
            {t('masterData.balanceWarning.balance', { kg: formatKgText(row.balanceKg) })}
            {' · '}
            {t('masterData.balanceWarning.inTransit', { kg: formatKgText(row.inTransitKg) })}
          </Text>
        ))}
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            {t('masterData.balanceWarning.cancel')}
          </Button>
          <Button color="red" onClick={onConfirm}>
            {t('masterData.balanceWarning.confirm')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
