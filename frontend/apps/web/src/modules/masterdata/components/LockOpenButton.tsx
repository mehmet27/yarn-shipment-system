import { useTranslation } from '@iplik/i18n';
import { Button, Stack, Text } from '@iplik/ui';

/** "Kaliteyi kilitle" düğmesi ve kilidin ne yaptığını anlatan tek cümlelik ipucu. */
export function LockOpenButton({ onOpen }: { onOpen: () => void }) {
  const { t } = useTranslation();
  return (
    <Stack gap={4}>
      <Button variant="default" onClick={onOpen}>
        {t('lots.lock.open')}
      </Button>
      <Text size="xs" c="dimmed">
        {t('lots.lock.hint')}
      </Text>
    </Stack>
  );
}
