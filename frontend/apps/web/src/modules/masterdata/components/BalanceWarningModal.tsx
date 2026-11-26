import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Modal, Stack, Text } from '@iplik/ui';
import { formatKgText } from '@iplik/units';
import type { PendingWarning } from '../hooks/useDeactivateGuard';

export type BalanceWarningModalProps = {
  warning: PendingWarning | undefined;
  onConfirm: () => void;
  onClose: () => void;
};

type Translate = ReturnType<typeof useTranslation>['t'];

const titleOf = (warning: PendingWarning | undefined) =>
  warning?.fabricTypes === undefined
    ? 'masterData.balanceWarning.title'
    : 'masterData.recipeWarning.title';

function messageOf(t: Translate, warning: PendingWarning | undefined): string {
  const title = warning?.record.label ?? '';
  if (warning?.fabricTypes !== undefined) {
    const fabricTypes = warning.fabricTypes.join(', ');
    return t('masterData.recipeWarning.message', { title, fabricTypes });
  }
  const key =
    warning?.rows === undefined
      ? 'masterData.balanceWarning.unknown'
      : 'masterData.balanceWarning.message';
  return t(key, { title });
}

/** KR-18: bakiyesi olan fasoncu pasife alınırken uyarır; karar yöneticidedir. */
export function BalanceWarningModal({ warning, onConfirm, onClose }: BalanceWarningModalProps) {
  const { t } = useTranslation();
  return (
    <Modal opened={warning !== undefined} onClose={onClose} title={t(titleOf(warning))}>
      <Stack>
        <Alert color="orange" role="alert">
          {messageOf(t, warning)}
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
