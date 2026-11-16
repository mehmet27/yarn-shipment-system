import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';

/** Tartı kaydedildi ama fotoğraf yüklenemedi: tartı tekrar girilemez, fiş dosyadan eklenir. */
export function PhotoFailedNotice({ onContinue }: { onContinue: () => void }) {
  const { t } = useTranslation();
  return (
    <Alert color="orange">
      <Stack gap="xs">
        <Text>{t('dispatch.photoFailed')}</Text>
        <Button variant="default" w="fit-content" onClick={onContinue}>
          {t('dispatch.continue')}
        </Button>
      </Stack>
    </Alert>
  );
}
