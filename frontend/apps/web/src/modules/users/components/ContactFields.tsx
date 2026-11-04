import { useTranslation } from '@iplik/i18n';
import { TextInput } from '@iplik/ui';
import type { AccountFieldsProps } from './AccountFields';

/** Telefon (kod buraya gider) ve isteğe bağlı e-posta. */
export function ContactFields({ draft, onChange: change, errorText }: AccountFieldsProps) {
  const { t } = useTranslation();
  return (
    <>
      <TextInput
        type="tel"
        label={t('users.field.phone')}
        description={t('users.create.phoneHint')}
        value={draft.phone}
        onChange={(event) => {
          change({ phone: event.currentTarget.value });
        }}
        error={errorText('phone')}
        withAsterisk
      />
      <TextInput
        label={t('users.field.emailOptional')}
        value={draft.email}
        onChange={(event) => {
          change({ email: event.currentTarget.value.trim() });
        }}
        error={errorText('email')}
      />
    </>
  );
}
