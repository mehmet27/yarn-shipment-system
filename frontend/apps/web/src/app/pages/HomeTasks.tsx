import { useTranslation } from '@iplik/i18n';
import { Button, EmptyState, Stack, Title } from '@iplik/ui';
import { Link } from 'react-router';
import { Can } from '@/modules/auth';
import { PendingFabric, PendingInvoices } from '@/modules/balance';
import { PendingDiscrepancies } from '@/modules/discrepancy';
import { PendingDeliveries, PendingDispatches, PendingWeightDecisions } from '@/modules/shipment';
import { useHomeTasks } from './useHomeTasks';

/**
 * Ana sayfanın "Sizi bekleyen işler" bölümü: kullanıcının yapması gereken işler, en acili üstte. Her kart yalnızca
 * yetkisi olan kullanıcıya ve bekleyen iş varsa görünür; hiç iş yoksa bölüm bunu açıkça söyler (boş sayfa kalmaz).
 */
export function HomeTasks() {
  const { t } = useTranslation();
  const tasks = useHomeTasks();
  if (!tasks.hasDuty) return null;
  return (
    <Stack component="section" aria-label={t('home.tasks')} data-testid="home-tasks">
      <Title order={2}>{t('home.tasks')}</Title>
      <PendingWeightDecisions />
      <Can permission="DISCREPANCY_DECIDE">
        <PendingDiscrepancies />
      </Can>
      <PendingDispatches />
      <PendingDeliveries />
      <PendingInvoices />
      <PendingFabric />
      {!tasks.isLoading && tasks.total === 0 && (
        <EmptyState
          title={t('home.allClear')}
          description={t(`home.allClearHint.${tasks.duty}`)}
          action={
            <Can
              permission="SHIPMENT_CREATE"
              fallback={
                <Button component={Link} to="/panel/sevkiyat" variant="default">
                  {t('home.openShipments')}
                </Button>
              }
            >
              <Button component={Link} to="/panel/sevkiyat/yeni">
                {t('create.open')}
              </Button>
            </Can>
          }
        />
      )}
    </Stack>
  );
}
