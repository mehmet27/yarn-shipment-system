import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';

/** Geçici şifre yalnızca bu yanıtta gelir; pencere kapanınca bir daha görülemez. */
export function LoginAccountResult({
  username,
  temporaryPassword,
  onClose,
}: {
  username: string;
  temporaryPassword: string;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  return (
    <Stack>
      <Alert color="green">{t('masterData.loginAccount.created')}</Alert>
      <Text>
        {t('masterData.loginAccount.username')}: <b>{username}</b>
      </Text>
      <Text>
        {t('masterData.loginAccount.password')}: <b>{temporaryPassword}</b>
      </Text>
      <Alert color="yellow">{t('masterData.loginAccount.showOnce')}</Alert>
      <Button onClick={onClose}>{t('masterData.loginAccount.close')}</Button>
    </Stack>
  );
}
