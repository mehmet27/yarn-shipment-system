import type { LoginAccountView } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Modal, Stack, Text, useDisclosure } from '@iplik/ui';
import { useSetAccountEnabled } from '../hooks/useAccountMutations';

/**
 * Hesabı pasife alır ya da yeniden açar. Pasife alınan kişi giremez ve açık oturumları kapanır; veri silinmez.
 * Pasife alma geri alınabilir ama kişiyi hemen dışarı attığı için önce onay istenir; yeniden açma onaysız çalışır.
 * Hata düğmenin altında kalır.
 */
export function ToggleEnabledButton({ account }: { account: LoginAccountView }) {
  const { t } = useTranslation();
  const mutation = useSetAccountEnabled(account.id);
  const [isConfirming, { open, close }] = useDisclosure(false);
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
          if (account.enabled) {
            open();
          } else {
            mutation.mutate(true);
          }
        }}
      >
        {t(account.enabled ? 'users.disable.action' : 'users.enable.action')}
      </Button>
      <Modal opened={isConfirming} onClose={close} title={t('users.disable.confirmTitle')}>
        <Stack>
          <Text size="sm">{t('users.disable.confirmIntro', { username: account.username })}</Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={close}>
              {t('users.disable.cancel')}
            </Button>
            <Button
              color="red"
              loading={mutation.isPending}
              onClick={() => {
                mutation.mutate(false, { onSettled: close });
              }}
            >
              {t('users.disable.confirm')}
            </Button>
          </Group>
        </Stack>
      </Modal>
      {errorCode !== undefined && (
        <Alert color="red">
          {t(errorCode === 'LOGIN_ACCOUNT_NOT_FOUND' ? 'users.notFound' : 'users.disable.error')}
        </Alert>
      )}
    </Stack>
  );
}
