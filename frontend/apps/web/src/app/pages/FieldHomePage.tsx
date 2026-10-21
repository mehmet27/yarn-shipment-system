import { useTranslation } from '@iplik/i18n';
import { Title } from '@iplik/ui';
import { useCurrentUser } from '@/modules/auth';

export function FieldHomePage() {
  const { t } = useTranslation();
  const { data: user } = useCurrentUser();
  return <Title order={3}>{t('home.welcome', { name: user?.fullName ?? '' })}</Title>;
}
