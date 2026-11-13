import { useTranslation } from '@iplik/i18n';
import { Button, SimpleGrid, Stack, Text, Title } from '@iplik/ui';
import { Link } from 'react-router';
import { Can, useCurrentUser } from '@/modules/auth';
import { DashboardSection } from '@/modules/report';
import { PendingWeightDecisions } from '@/modules/shipment';

export function PanelHomePage() {
  const { t } = useTranslation();
  const { data: user } = useCurrentUser();
  return (
    <Stack>
      <Title order={1} size="h2">
        {t('home.welcome', { name: user?.fullName ?? '' })}
      </Title>
      <Can permission="SHIPMENT_VIEW">
        <PendingWeightDecisions />
      </Can>
      <Can
        permission="REPORT_VIEW"
        fallback={
          <Stack>
            <Text c="dimmed">{t('home.shortcuts')}</Text>
            <SimpleGrid cols={{ base: 1, sm: 2 }}>
              <Button component={Link} to="/panel/sevkiyat" size="lg" variant="light">
                {t('nav.shipments')}
              </Button>
            </SimpleGrid>
          </Stack>
        }
      >
        <DashboardSection />
      </Can>
    </Stack>
  );
}
