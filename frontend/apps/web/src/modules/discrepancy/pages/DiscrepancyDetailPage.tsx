import { useTranslation } from '@iplik/i18n';
import { Anchor, Group, Stack, Title } from '@iplik/ui';
import { Link, useParams } from 'react-router';
import { ClaimOfDiscrepancy } from '@/modules/claim';
import { QueryState } from '@/modules/shipment';
import { ActivityList } from '../components/ActivityList';
import { AssignToMeButton } from '../components/AssignToMeButton';
import { DiscrepancyBadges } from '../components/DiscrepancyBadges';
import { DiscrepancyFacts } from '../components/DiscrepancyFacts';
import { ResolveForm } from '../components/ResolveForm';
import { useDiscrepancy } from '../hooks/useDiscrepancy';

/** Kararlar `allowedActions`'a göre görünür (FE-SEC-03); rol adına bakılmaz. */
export function DiscrepancyDetailPage() {
  const { t } = useTranslation();
  const { discrepancyId = '' } = useParams();
  const query = useDiscrepancy(discrepancyId);
  return (
    <QueryState query={query}>
      {(discrepancy) => (
        <Stack>
          <Anchor component={Link} to="/panel/uyusmazliklar">
            {t('discrepancy.back')}
          </Anchor>
          <Group justify="space-between">
            <Title order={1} size="h2">
              {discrepancy.discrepancyNo}
            </Title>
            <DiscrepancyBadges discrepancy={discrepancy} />
          </Group>
          <Anchor component={Link} to={`/panel/sevkiyat/${discrepancy.shipmentId}`}>
            {t('discrepancy.openShipment')}
          </Anchor>
          <DiscrepancyFacts discrepancy={discrepancy} />
          {discrepancy.ruleCode === 'QUALITY_CLAIM' && (
            <ClaimOfDiscrepancy
              shipmentId={discrepancy.shipmentId}
              discrepancyId={discrepancy.id}
            />
          )}
          {discrepancy.allowedActions?.includes('ASSIGN') && (
            <AssignToMeButton discrepancyId={discrepancy.id} />
          )}
          {discrepancy.allowedActions?.includes('RESOLVE') && (
            <ResolveForm discrepancyId={discrepancy.id} />
          )}
          <ActivityList discrepancyId={discrepancy.id} />
        </Stack>
      )}
    </QueryState>
  );
}
