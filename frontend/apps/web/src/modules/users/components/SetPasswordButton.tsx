import type { LoginAccountView } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Modal, PasswordInput, Stack, Text, useDisclosure } from '@iplik/ui';
import { useState } from 'react';
import { MIN_PASSWORD_LENGTH } from '../accountDraft';
import { useSetAccountPassword } from '../hooks/useAccountMutations';

const KNOWN_CODES = ['LOGIN_ACCOUNT_NOT_FOUND', 'WEAK_PASSWORD', 'LOGIN_ACCOUNT_UNAVAILABLE'];

function errorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `users.setPassword.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

/**
 * Yönetici kullanıcıya yeni geçici şifre verir (unutulan şifre dahil); SMS ya da e-posta yoktur. Kişi bir sonraki
 * girişte şifresini değiştirir, açık oturumları kapanır.
 */
export function SetPasswordButton({ account }: { account: LoginAccountView }) {
  const { t } = useTranslation();
  const [isOpen, { open, close }] = useDisclosure(false);
  const [password, setPassword] = useState('');
  const mutation = useSetAccountPassword(account.id);
  const dismiss = () => {
    mutation.reset();
    setPassword('');
    close();
  };
  return (
    <>
      <Button
        variant="default"
        onClick={open}
        aria-label={t('users.setPassword.open', { username: account.username })}
      >
        {t('users.setPassword.action')}
      </Button>
      <Modal opened={isOpen} onClose={dismiss} title={t('users.setPassword.title')}>
        <Stack>
          {mutation.isSuccess ? (
            <>
              <Alert color="green">
                {t('users.setPassword.done', { username: account.username })}
              </Alert>
              <Button onClick={dismiss}>{t('masterData.loginAccount.close')}</Button>
            </>
          ) : (
            <>
              <Text size="sm">{t('users.setPassword.intro', { username: account.username })}</Text>
              <PasswordInput
                label={t('users.field.password')}
                description={t('users.create.passwordHint')}
                value={password}
                onChange={(event) => {
                  setPassword(event.currentTarget.value);
                }}
                autoComplete="new-password"
              />
              {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
              <Button
                loading={mutation.isPending}
                disabled={password.length < MIN_PASSWORD_LENGTH}
                onClick={() => {
                  mutation.mutate(password);
                }}
              >
                {t('users.setPassword.submit')}
              </Button>
            </>
          )}
        </Stack>
      </Modal>
    </>
  );
}
