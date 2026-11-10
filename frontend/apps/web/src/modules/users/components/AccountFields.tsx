import { useTranslation } from '@iplik/i18n';
import { Stack, TextInput } from '@iplik/ui';
import { SiteField } from '@/modules/masterdata';
import type { AccountDraft, AccountField } from '../accountDraft';
import { StaffRoleSelect } from './StaffRoleSelect';
import { ContactFields } from './ContactFields';

export type AccountFieldsProps = {
  draft: AccountDraft;
  onChange: (patch: Partial<AccountDraft>) => void;
  errorText: (field: AccountField) => string | undefined;
};

/**
 * Hesap açma formunun alanları; hata metinleri üstten gelir. Şifre alanı yoktur: kişi SMS koduyla kendisi belirler.
 */
export function AccountFields({ draft, onChange: change, errorText }: AccountFieldsProps) {
  const { t } = useTranslation();
  const kind = draft.kind;
  return (
    <Stack>
      <StaffRoleSelect
        value={kind}
        onChange={(next) => {
          change({ kind: next });
        }}
        error={errorText('role')}
      />
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
      <ContactFields draft={draft} onChange={change} errorText={errorText} />
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
