import type { LoginAccountView } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Modal, Stack, Text, useDisclosure } from '@iplik/ui';
import { useStartPasswordReset } from '../hooks/useAccountMutations';

const KNOWN_CODES = ['LOGIN_ACCOUNT_NOT_FOUND', 'SMS_UNAVAILABLE', 'INVALID_MASTER_DATA'];

function errorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `users.resetPassword.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

/**
 * Kullanıcının telefonuna şifre sıfırlama kodu gönderir; şifreyi yönetici görmez, kişi kodla kendisi belirler.
 * Telefon kayıtlı değilse düğme kapalıdır.
 */
export function ResetPasswordButton({ account }: { account: LoginAccountView }) {
  const { t } = useTranslation();
  const [isOpen, { open, close }] = useDisclosure(false);
  const mutation = useStartPasswordReset(account.id);
  const phone = account.phone;
  const dismiss = () => {
    mutation.reset();
    close();
  };
  return (
    <>
      <Button
        variant="default"
        onClick={open}
        disabled={phone === undefined}
        aria-label={t('users.resetPassword.open', { username: account.username })}
      >
        {t('users.resetPassword.action')}
      </Button>
      <Modal opened={isOpen} onClose={dismiss} title={t('users.resetPassword.title')}>
        <Stack>
          {mutation.isSuccess ? (
            <>
              <Alert color="green">{t('users.resetPassword.sent', { phone: phone ?? '' })}</Alert>
              <Button onClick={dismiss}>{t('masterData.loginAccount.close')}</Button>
            </>
          ) : (
            <>
              <Text size="sm">
                {t('users.resetPassword.intro', { username: account.username, phone: phone ?? '' })}
              </Text>
              {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
              <Button
                loading={mutation.isPending}
                onClick={() => {
                  mutation.mutate();
                }}
              >
                {t('users.resetPassword.submit')}
              </Button>
            </>
          )}
        </Stack>
      </Modal>
    </>
  );
}
