import { useTranslation } from '@iplik/i18n';
import { Stack, Title } from '@iplik/ui';
import { Can, useCurrentUser } from '@/modules/auth';
import { DashboardSection } from '@/modules/report';

export function PanelHomePage() {
  const { t } = useTranslation();
  const { data: user } = useCurrentUser();
  return (
    <Stack>
      <Title order={1} size="h2">
        {t('home.welcome', { name: user?.fullName ?? '' })}
      </Title>
      <Can permission="REPORT_VIEW">
        <DashboardSection />
      </Can>
    </Stack>
  );
}
