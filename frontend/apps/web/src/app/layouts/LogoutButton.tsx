import { logout, LOGIN_PATH } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button } from '@iplik/ui';

async function logoutAndRedirect(): Promise<void> {
  await logout();
  window.location.assign(LOGIN_PATH);
}

export function LogoutButton() {
  const { t } = useTranslation();
  return (
    <Button variant="subtle" onClick={() => void logoutAndRedirect()}>
      {t('app.logout')}
    </Button>
  );
}
