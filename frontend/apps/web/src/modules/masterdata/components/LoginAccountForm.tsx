import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useCreateLoginAccount } from '../hooks/useCreateLoginAccount';
import type { LoginAccountKind } from '../hooks/useCreateLoginAccount';
import { LoginAccountResult } from './LoginAccountResult';
import { SiteSwitches } from './SiteSwitches';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const USERNAME_PATTERN = /^[a-z0-9][a-z0-9._-]{2,63}$/;
const KNOWN_CODES = ['LOGIN_ACCOUNT_EXISTS', 'LOGIN_ACCOUNT_UNAVAILABLE', 'INVALID_MASTER_DATA'];

function errorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `masterData.loginAccount.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

/** Giriş adı, isteğe bağlı e-posta ve görülebilecek lokasyonlarla Keycloak hesabı açar. */
export function LoginAccountForm({
  kind,
  recordId,
  onClose,
}: {
  kind: LoginAccountKind;
  recordId: string;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [siteIds, setSiteIds] = useState<readonly string[]>([]);
  const mutation = useCreateLoginAccount(kind, recordId);
  if (mutation.isSuccess) {
    return <LoginAccountResult {...mutation.data} onClose={onClose} />;
  }
  const isValid =
    USERNAME_PATTERN.test(username) && EMAIL_PATTERN.test(email) && siteIds.length > 0;
  return (
    <Stack>
      <TextInput
        label={t('masterData.loginAccount.username')}
        description={t('masterData.loginAccount.usernameHint')}
        value={username}
        onChange={(event) => {
          setUsername(event.currentTarget.value.trim());
        }}
        withAsterisk
        data-autofocus
      />
      <TextInput
        label={t('masterData.loginAccount.email')}
        withAsterisk
        value={email}
        onChange={(event) => {
          setEmail(event.currentTarget.value.trim());
        }}
      />
      <Text fw={500}>{t('masterData.loginAccount.sites')}</Text>
      <SiteSwitches selected={siteIds} onChange={setSiteIds} />
      {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
      <Button
        disabled={!isValid}
        loading={mutation.isPending}
        onClick={() => {
          mutation.mutate({ username, email, siteIds: [...siteIds] });
        }}
      >
        {t('masterData.loginAccount.submit')}
      </Button>
    </Stack>
  );
}
