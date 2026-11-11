import { useTranslation } from '@iplik/i18n';
import { PasswordInput, Stack, TextInput } from '@iplik/ui';
import type { AccountDraft, AccountField } from '../accountDraft';
import { needsWorkArea } from '../staffRoles';
import { StaffRoleSelect } from './StaffRoleSelect';
import { WorkAreaSelect } from './WorkAreaSelect';

export type AccountFieldsProps = {
  draft: AccountDraft;
  onChange: (patch: Partial<AccountDraft>) => void;
  errorText: (field: AccountField) => string | undefined;
};

/** Hesap açma formunun alanları; hata metinleri üstten gelir. İlk şifreyi yönetici yazar (SMS/e-posta yok). */
export function AccountFields({ draft, onChange: change, errorText }: AccountFieldsProps) {
  const { t } = useTranslation();
  return (
    <Stack>
      <StaffRoleSelect
        value={draft.kind}
        onChange={(next) => {
          change({ kind: next });
        }}
        error={errorText('role')}
      />
      {needsWorkArea(draft.kind) && (
        <WorkAreaSelect
          value={draft.workArea}
          onChange={(next) => {
            change({ workArea: next });
          }}
          error={errorText('workArea')}
        />
      )}
      <TextInput
        label={t('masterData.loginAccount.username')}
        description={t('masterData.loginAccount.usernameHint')}
        value={draft.username}
        onChange={(event) => {
          change({ username: event.currentTarget.value.trim() });
        }}
        error={errorText('username')}
        withAsterisk
        data-autofocus
      />
      <TextInput
        label={t('users.field.fullName')}
        value={draft.fullName}
        onChange={(event) => {
          change({ fullName: event.currentTarget.value });
        }}
        error={errorText('fullName')}
        withAsterisk
      />
      <PasswordInput
        label={t('users.field.password')}
        description={t('users.create.passwordHint')}
        value={draft.password}
        onChange={(event) => {
          change({ password: event.currentTarget.value });
        }}
        error={errorText('password')}
        autoComplete="new-password"
        withAsterisk
      />
    </Stack>
  );
}
