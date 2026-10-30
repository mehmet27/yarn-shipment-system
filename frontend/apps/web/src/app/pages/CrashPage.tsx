import { useTranslation } from '@iplik/i18n';
import { Button, Center, Group, Stack, Text } from '@iplik/ui';

/**
 * Çizim ya da modül yükleme hatasında beyaz ekran yerine görünür (H-25). Yönlendirici dışında da
 * çalışsın diye bağlantılar düz `<a>`dır; "Yeniden dene" sayfayı baştan yükler.
 */
export function CrashPage() {
  const { t } = useTranslation();
  return (
    <Center h="100vh" p="md">
      <Stack align="center" maw={420} role="alert">
        <Text size="lg" ta="center">
          {t('error.crashed')}
        </Text>
        <Group>
          <Button
            onClick={() => {
              window.location.reload();
            }}
          >
            {t('error.retry')}
          </Button>
          <Button component="a" href="/" variant="default">
            {t('error.backHome')}
          </Button>
        </Group>
      </Stack>
    </Center>
  );
}
