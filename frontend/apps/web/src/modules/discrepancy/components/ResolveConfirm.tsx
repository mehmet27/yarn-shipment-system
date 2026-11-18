import { useTranslation } from '@iplik/i18n';
import { Button, Group, Paper, Stack, Text } from '@iplik/ui';

export type ResolveConfirmProps = {
  /** Onay sorusu; seçilen kararı söyler. */
  text: string;
  loading: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

/** Karar geri alınamaz: kaydetmeden önce satır içi onay istenir. */
export function ResolveConfirm({ text, loading, onConfirm, onCancel }: ResolveConfirmProps) {
  const { t } = useTranslation();
  return (
    <Paper withBorder p="sm" role="group" aria-label={t('discrepancy.resolve.confirmTitle')}>
      <Stack gap="sm">
        <Text size="sm">{text}</Text>
        <Group grow>
          <Button color="orange" loading={loading} onClick={onConfirm}>
            {t('discrepancy.resolve.confirmYes')}
          </Button>
          <Button variant="default" onClick={onCancel}>
            {t('discrepancy.resolve.confirmNo')}
          </Button>
        </Group>
      </Stack>
    </Paper>
  );
}
