import { changePasswordUrl } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button } from '@iplik/ui';

/** Kişinin kendi şifresini değiştirmesi: Keycloak'ın şifre ekranına gider, şifre bu uygulamadan geçmez. */
export function ChangePasswordButton() {
  const { t } = useTranslation();
  return (
    <Button
      variant="subtle"
      color="white"
      onClick={() => {
        window.location.assign(changePasswordUrl());
      }}
    >
      {t('app.changePassword')}
    </Button>
  );
}
