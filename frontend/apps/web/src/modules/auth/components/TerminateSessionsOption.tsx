import { useTranslation } from '@iplik/i18n';
import { Checkbox } from '@iplik/ui';

export type TerminateSessionsOptionProps = {
  isChecked: boolean;
  onChange: (isChecked: boolean) => void;
};

/** Şifre değişirken kişinin diğer cihaz ve tarayıcılardaki oturumlarını kapatıp kapatmayacağı. */
export function TerminateSessionsOption({ isChecked, onChange }: TerminateSessionsOptionProps) {
  const { t } = useTranslation();
  return (
    <Checkbox
      label={t('auth.changePassword.terminateOthers')}
      description={t('auth.changePassword.terminateOthersHint')}
      checked={isChecked}
      onChange={(event) => {
        onChange(event.currentTarget.checked);
      }}
    />
  );
}
