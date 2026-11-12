import type { LoginAccountView } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useChangeAccountName } from '../hooks/useAccountMutations';

const KNOWN_CODES = ['LOGIN_ACCOUNT_NOT_FOUND', 'LOGIN_ACCOUNT_UNAVAILABLE', 'INVALID_MASTER_DATA'];
const MIN_NAME_LENGTH = 2;

function errorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `users.changeName.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

/** Yeni ad soyadı yazdırır ve kaydeder; hata pencerede kalır, başarıda `onClose` çağrılır. */
export function ChangeNameForm({
  account,
  onClose,
}: {
  account: LoginAccountView;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const [fullName, setFullName] = useState(account.fullName ?? '');
  const mutation = useChangeAccountName(account.id);
  const trimmed = fullName.trim();
  const isInvalid = trimmed.length < MIN_NAME_LENGTH;
  const isUnchanged = trimmed === (account.fullName ?? '');
  return (
    <Stack>
      <Text size="sm">{t('users.changeName.intro', { username: account.username })}</Text>
      <TextInput
        label={t('users.field.fullName')}
        value={fullName}
        onChange={(event) => {
          setFullName(event.currentTarget.value);
        }}
        maxLength={200}
        withAsterisk
        data-autofocus
      />
      {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
      <Button
        loading={mutation.isPending}
        disabled={isInvalid || isUnchanged}
        onClick={() => {
          mutation.mutate(trimmed, { onSuccess: onClose });
        }}
      >
        {t('users.changeName.submit')}
      </Button>
    </Stack>
  );
}
