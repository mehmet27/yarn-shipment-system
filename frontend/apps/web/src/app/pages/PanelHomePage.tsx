import { useTranslation } from '@iplik/i18n';
import { Title } from '@iplik/ui';
import { useCurrentUser } from '@/modules/auth';

export function PanelHomePage() {
  const { t } = useTranslation();
  const { data: user } = useCurrentUser();
  return <Title order={2}>{t('home.welcome', { name: user?.fullName ?? '' })}</Title>;
}
