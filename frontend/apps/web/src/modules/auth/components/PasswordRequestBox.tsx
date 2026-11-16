import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';
import { useRequestPassword } from '../hooks/useRequestPassword';

const ADMIN_USERNAME = 'admin';

export type PasswordRequestBoxProps = { username: string };

/** Üç hatalı denemeden sonra (ya da hesap kilitlenince) görünür: adminden şifre talep edilir; `admin` hesabı için talep yerine açıklama gösterilir. */
export function PasswordRequestBox({ username }: PasswordRequestBoxProps) {
  const { t } = useTranslation();
  const mutation = useRequestPassword();
  if (username.trim().toLowerCase() === ADMIN_USERNAME) {
    return <Alert color="yellow">{t('auth.passwordRequest.adminHint')}</Alert>;
  }
  if (mutation.isSuccess) {
    return <Alert color="green">{t('auth.passwordRequest.sent')}</Alert>;
  }
  return (
    <Stack gap="xs">
      <Text size="sm">{t('auth.passwordRequest.hint')}</Text>
      {mutation.isError && <Alert color="red">{t('auth.passwordRequest.failed')}</Alert>}
      <Button
        variant="light"
        loading={mutation.isPending}
        onClick={() => {
          mutation.mutate(username);
        }}
      >
        {t('auth.passwordRequest.submit')}
      </Button>
    </Stack>
  );
}
