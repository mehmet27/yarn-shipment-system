import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { useCreateAccount } from '../hooks/useAccountMutations';
import { EMPTY_DRAFT, invalidFields, toSubmission } from '../accountDraft';
import type { AccountDraft, AccountField } from '../accountDraft';
import { AccountFields } from './AccountFields';

const KNOWN_CODES = [
  'LOGIN_ACCOUNT_EXISTS',
  'LOGIN_ACCOUNT_UNAVAILABLE',
  'SMS_UNAVAILABLE',
  'INVALID_MASTER_DATA',
  'MASTER_DATA_NOT_FOUND',
];

function errorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `users.create.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

/**
 * Giriş adı, telefon, rol ve lokasyonla hesap açar. Şifre sorulmaz ve gösterilmez: kişiye SMS'le kod gider, şifresini
 * kendisi belirler. Düğme hep açıktır; basınca eksik alanların altında ne yapılacağı yazar.
 */
export function CreateAccountForm({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<AccountDraft>(EMPTY_DRAFT);
  const [isSubmitted, setSubmitted] = useState(false);
  const mutation = useCreateAccount();
  const change = (patch: Partial<AccountDraft>) => {
    setDraft((current) => ({ ...current, ...patch }));
  };
  if (mutation.isSuccess) {
    return (
      <Stack>
        <Alert color="green">{t('users.create.sent', { phone: draft.phone })}</Alert>
        <Text size="sm" c="dimmed">
          {t('users.create.sentHint')}
        </Text>
        <Button onClick={onClose}>{t('masterData.loginAccount.close')}</Button>
      </Stack>
    );
  }
  const invalid = invalidFields(draft);
  const errorText = (field: AccountField) =>
    isSubmitted && invalid.has(field) ? t(`users.create.invalid.${field}`) : undefined;
  return (
    <Stack>
      <AccountFields draft={draft} onChange={change} errorText={errorText} />
      {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
      <Button
        loading={mutation.isPending}
        onClick={() => {
          setSubmitted(true);
          const submission = toSubmission(draft);
          if (invalid.size > 0 || submission === undefined) return;
          mutation.mutate(submission);
        }}
      >
        {t('users.create.submit')}
      </Button>
    </Stack>
  );
}
