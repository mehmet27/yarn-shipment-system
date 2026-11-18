import { useTranslation } from '@iplik/i18n';
import { Button, Group, Paper, Stack, Text } from '@iplik/ui';
import { formatKgText } from './format';

export type FabricConfirmProps = {
  fabricType: string;
  netKg: string;
  loading: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

/** Kumaş girişi sonradan değiştirilemez: kaydetmeden önce özet gösterilip satır içi onay istenir. */
export function FabricConfirm({
  fabricType,
  netKg,
  loading,
  onConfirm,
  onCancel,
}: FabricConfirmProps) {
  const { t } = useTranslation();
  return (
    <Paper withBorder p="sm" role="group" aria-label={t('settlement.fabric.confirmTitle')}>
      <Stack gap="sm">
        <Text size="sm">
          {t('settlement.fabric.confirmText', { kg: formatKgText(netKg), type: fabricType })}
        </Text>
        <Group grow>
          <Button loading={loading} onClick={onConfirm}>
            {t('settlement.fabric.confirmYes')}
          </Button>
          <Button variant="default" onClick={onCancel}>
            {t('settlement.fabric.confirmNo')}
          </Button>
        </Group>
      </Stack>
    </Paper>
  );
}
