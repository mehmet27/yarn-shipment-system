import { useTranslation } from '@iplik/i18n';
import { Button, Group, Paper, Stack, Text } from '@iplik/ui';

export type SaveConfirmProps = {
  isOpen: boolean;
  isLoading: boolean;
  submitLabel: string;
  onSubmit: () => void;
  onConfirm: () => void;
  onCancel: () => void;
};

/** Sonradan değiştirilemeyen kayıtlarda iki adım: önce kaydet düğmesi, sonra "Evet, kaydet" / "Vazgeç". */
export function SaveConfirm(props: SaveConfirmProps) {
  const { t } = useTranslation();
  if (!props.isOpen) {
    return (
      <Button w="fit-content" loading={props.isLoading} onClick={props.onSubmit}>
        {props.submitLabel}
      </Button>
    );
  }
  return (
    <Paper withBorder p="sm" role="group" aria-label={t('common.saveConfirm.title')}>
      <Stack gap="sm">
        <Text size="sm">{t('common.saveConfirm.text')}</Text>
        <Group>
          <Button loading={props.isLoading} onClick={props.onConfirm}>
            {t('common.saveConfirm.yes')}
          </Button>
          <Button variant="default" onClick={props.onCancel}>
            {t('common.saveConfirm.no')}
          </Button>
        </Group>
      </Stack>
    </Paper>
  );
}
