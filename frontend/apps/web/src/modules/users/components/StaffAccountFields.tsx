import { useTranslation } from '@iplik/i18n';
import { Stack, TextInput } from '@iplik/ui';
import { SiteField } from '@/modules/masterdata';
import type { StaffAccountDraft, StaffAccountField } from '../staffAccountDraft';
import { StaffRoleSelect } from './StaffRoleSelect';

export type StaffAccountFieldsProps = {
  draft: StaffAccountDraft;
  onChange: (patch: Partial<StaffAccountDraft>) => void;
  errorText: (field: StaffAccountField) => string | undefined;
};

/** Hesap açma formunun alanları; hata metinleri üstten gelir. */
export function StaffAccountFields({
  draft,
  onChange: change,
  errorText,
}: StaffAccountFieldsProps) {
  const { t } = useTranslation();
  return (
    <Stack>
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
      <TextInput
        label={t('users.field.email')}
        description={t('users.create.emailHint')}
        value={draft.email}
        onChange={(event) => {
          change({ email: event.currentTarget.value.trim() });
        }}
        error={errorText('email')}
        withAsterisk
      />
      <StaffRoleSelect
        value={draft.role}
        onChange={(role) => {
          change({ role });
        }}
        error={errorText('role')}
      />
      <SiteField
        selected={draft.siteIds}
        onChange={(siteIds) => {
          change({ siteIds });
        }}
        error={errorText('sites')}
      />
    </Stack>
  );
}
