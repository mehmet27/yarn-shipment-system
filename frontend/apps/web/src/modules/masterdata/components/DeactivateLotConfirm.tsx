import { useTranslation } from '@iplik/i18n';
import { Button, Group, Paper, Stack, Text } from '@iplik/ui';

export type DeactivateLotConfirmProps = {
  lotNo: string;
  isLoading: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

/** "Pasife al" yanlışlıkla basılmasın diye satır içi onay ister. */
export function DeactivateLotConfirm({
  lotNo,
  isLoading,
  onConfirm,
  onCancel,
}: DeactivateLotConfirmProps) {
  const { t } = useTranslation();
  return (
    <Paper withBorder p="sm">
      <Stack gap="sm">
        <Text size="sm">{t('lots.deactivateQuestion', { lotNo })}</Text>
        <Group grow>
          <Button color="red" loading={isLoading} onClick={onConfirm}>
            {t('lots.deactivateConfirm')}
          </Button>
          <Button variant="default" onClick={onCancel}>
            {t('lots.lock.dismiss')}
          </Button>
        </Group>
      </Stack>
    </Paper>
  );
}
