import type { LoginAccountView } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack } from '@iplik/ui';
import { useSetAccountEnabled } from '../hooks/useAccountMutations';

/**
 * Hesabı pasife alır ya da yeniden açar. Pasife alınan kişi giremez ve açık oturumları kapanır; veri silinmez.
 * Hata düğmenin altında kalır.
 */
export function ToggleEnabledButton({ account }: { account: LoginAccountView }) {
  const { t } = useTranslation();
  const mutation = useSetAccountEnabled(account.id);
  const errorCode = mutation.isError ? toProblem(mutation.error).code : undefined;
  return (
    <Stack gap={4}>
      <Button
        variant="default"
        loading={mutation.isPending}
        aria-label={t(account.enabled ? 'users.disable.open' : 'users.enable.open', {
          username: account.username,
        })}
        onClick={() => {
          mutation.mutate(!account.enabled);
        }}
      >
        {t(account.enabled ? 'users.disable.action' : 'users.enable.action')}
      </Button>
      {errorCode !== undefined && (
        <Alert color="red">
          {t(errorCode === 'LOGIN_ACCOUNT_NOT_FOUND' ? 'users.notFound' : 'users.disable.error')}
        </Alert>
      )}
    </Stack>
  );
}
