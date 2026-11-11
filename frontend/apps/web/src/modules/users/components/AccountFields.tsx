import { useTranslation } from '@iplik/i18n';
import { Stack, TextInput } from '@iplik/ui';
import { AccountOwnerSelect, SiteField } from '@/modules/masterdata';
import type { AccountDraft, AccountField } from '../accountDraft';
import { recordSlugOf } from '../staffRoles';
import { AccountKindSelect } from './AccountKindSelect';
import { ContactFields } from './ContactFields';

export type AccountFieldsProps = {
  draft: AccountDraft;
  onChange: (patch: Partial<AccountDraft>) => void;
  errorText: (field: AccountField) => string | undefined;
};

/**
 * Hesap açma formunun alanları; hata metinleri üstten gelir. Şoför ve fasoncuda ad kaydın kendisinden gelir, bu
 * yüzden kayıt seçilir; fabrika içi rollerde ad yazılır. Şifre alanı yoktur: kişi SMS koduyla kendisi belirler.
 */
export function AccountFields({ draft, onChange: change, errorText }: AccountFieldsProps) {
  const { t } = useTranslation();
  const kind = draft.kind;
  const slug = recordSlugOf(kind);
  return (
    <Stack>
      <AccountKindSelect
        value={kind}
        onChange={(next) => {
          change({ kind: next, recordId: undefined });
        }}
        error={errorText('role')}
      />
      {slug !== undefined && (
        <AccountOwnerSelect
          key={slug}
          kind={slug}
          value={draft.recordId}
          onChange={(recordId) => {
            change({ recordId });
          }}
          error={errorText('record')}
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
      {slug === undefined && (
        <TextInput
          label={t('users.field.fullName')}
          value={draft.fullName}
          onChange={(event) => {
            change({ fullName: event.currentTarget.value });
          }}
          error={errorText('fullName')}
          withAsterisk
        />
      )}
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
