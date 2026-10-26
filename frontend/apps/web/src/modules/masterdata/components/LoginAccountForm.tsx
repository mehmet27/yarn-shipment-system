import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useCreateLoginAccount } from '../hooks/useCreateLoginAccount';
import type { LoginAccountKind } from '../hooks/useCreateLoginAccount';
import { LoginAccountResult } from './LoginAccountResult';
import { SiteField } from './SiteField';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const USERNAME_PATTERN = /^[a-z0-9][a-z0-9._-]{2,63}$/;
const KNOWN_CODES = ['LOGIN_ACCOUNT_EXISTS', 'LOGIN_ACCOUNT_UNAVAILABLE', 'INVALID_MASTER_DATA'];

type Field = 'username' | 'email' | 'sites';

function errorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `masterData.loginAccount.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

/** Kurala uymayan alanlar; boşsa form gönderilebilir. */
function invalidFields(username: string, email: string, siteIds: readonly string[]): Set<Field> {
  const invalid = new Set<Field>();
  if (!USERNAME_PATTERN.test(username)) invalid.add('username');
  if (!EMAIL_PATTERN.test(email)) invalid.add('email');
  if (siteIds.length === 0) invalid.add('sites');
  return invalid;
}

/**
 * Giriş adı, e-posta ve görülebilecek lokasyonlarla Keycloak hesabı açar.
 * Düğme hep açıktır; basınca eksik alanların altında ne yapılacağı yazar.
 */
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
  const [isSubmitted, setSubmitted] = useState(false);
  const mutation = useCreateLoginAccount(kind, recordId);
  if (mutation.isSuccess) {
    return <LoginAccountResult {...mutation.data} onClose={onClose} />;
  }
  const invalid = invalidFields(username, email, siteIds);
  const errorText = (field: Field) =>
    isSubmitted && invalid.has(field) ? t(`masterData.loginAccount.invalid.${field}`) : undefined;
  return (
    <Stack>
      <TextInput
        label={t('masterData.loginAccount.username')}
        description={t('masterData.loginAccount.usernameHint')}
        value={username}
        onChange={(event) => {
          setUsername(event.currentTarget.value.trim());
        }}
        error={errorText('username')}
        withAsterisk
        data-autofocus
      />
      <TextInput
        label={t('masterData.loginAccount.email')}
        withAsterisk
        error={errorText('email')}
        value={email}
        onChange={(event) => {
          setEmail(event.currentTarget.value.trim());
        }}
      />
      <SiteField selected={siteIds} onChange={setSiteIds} error={errorText('sites')} />
      {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
      <Button
        loading={mutation.isPending}
        onClick={() => {
          setSubmitted(true);
          if (invalid.size > 0) return;
          mutation.mutate({ username, email, siteIds: [...siteIds] });
        }}
      >
        {t('masterData.loginAccount.submit')}
      </Button>
    </Stack>
  );
}
