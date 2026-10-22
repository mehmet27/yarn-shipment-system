import { logout } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button } from '@iplik/ui';

async function logoutAndRedirect(): Promise<void> {
  window.location.assign(await logout());
}

export function LogoutButton() {
  const { t } = useTranslation();
  return (
    <Button variant="subtle" color="white" onClick={() => void logoutAndRedirect()}>
      {t('app.logout')}
    </Button>
  );
}
