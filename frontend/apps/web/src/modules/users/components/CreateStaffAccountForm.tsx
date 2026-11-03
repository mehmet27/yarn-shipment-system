import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { useCreateStaffAccount } from '../hooks/useStaffAccountMutations';
import { EMPTY_DRAFT, invalidFields, toRequest } from '../staffAccountDraft';
import type { StaffAccountDraft, StaffAccountField } from '../staffAccountDraft';
import { StaffAccountFields } from './StaffAccountFields';

const KNOWN_CODES = ['LOGIN_ACCOUNT_EXISTS', 'LOGIN_ACCOUNT_UNAVAILABLE', 'INVALID_MASTER_DATA'];

function errorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `users.create.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

/**
 * Giriş adı, ad soyad, e-posta, rol ve lokasyonla fabrika içi hesap açar. Şifre sorulmaz ve gösterilmez; kişi
 * e-postadaki bağlantıdan kendi şifresini belirler. Düğme hep açıktır; basınca eksik alanların altında ne yapılacağı
 * yazar.
 */
export function CreateStaffAccountForm({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<StaffAccountDraft>(EMPTY_DRAFT);
  const [isSubmitted, setSubmitted] = useState(false);
  const mutation = useCreateStaffAccount();
  const change = (patch: Partial<StaffAccountDraft>) => {
    setDraft((current) => ({ ...current, ...patch }));
  };
  if (mutation.isSuccess) {
    return (
      <Stack>
        <Alert color="green">{t('users.create.sent', { email: draft.email })}</Alert>
        <Text size="sm" c="dimmed">
          {t('users.create.sentHint')}
        </Text>
        <Button onClick={onClose}>{t('masterData.loginAccount.close')}</Button>
      </Stack>
    );
  }
  const invalid = invalidFields(draft);
  const errorText = (field: StaffAccountField) =>
    isSubmitted && invalid.has(field) ? t(`users.create.invalid.${field}`) : undefined;
  return (
    <Stack>
      <StaffAccountFields draft={draft} onChange={change} errorText={errorText} />
      {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
      <Button
        loading={mutation.isPending}
        onClick={() => {
          setSubmitted(true);
          const request = toRequest(draft);
          if (invalid.size > 0 || request === undefined) return;
          mutation.mutate(request);
        }}
      >
        {t('users.create.submit')}
      </Button>
    </Stack>
  );
}
