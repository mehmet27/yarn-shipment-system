import { useTranslation } from '@iplik/i18n';
import { PageHero, Stack, Title } from '@iplik/ui';
import { useParams } from 'react-router';
import { BackLink, QueryState } from '@/modules/shared';
import { BalanceMatrix } from '../components/BalanceMatrix';
import { SubcontractorLists } from '../components/SubcontractorLists';
import { SubcontractorStats } from '../components/SubcontractorStats';
import { useSubcontractorOverview } from '../hooks/useSubcontractorOverview';

/**
 * Fasoncu sayfası (BALANCE_VIEW): bir fasoncuyla ilgili her şey tek ekranda. Yeni veri tutmaz; bakiye, sevkiyat,
 * fire dosyası, rapor ve uyuşmazlık ekranlarındaki bilgiyi o fasoncu için bir araya getirir.
 */
export function SubcontractorPage() {
  const { t } = useTranslation();
  const { subcontractorId = '' } = useParams();
  const overview = useSubcontractorOverview(subcontractorId);
  return (
    <Stack>
      <BackLink to="/panel/bakiye">{t('subcontractorPage.back')}</BackLink>
      <PageHero
        title={overview.title ?? t('subcontractorPage.title')}
        subtitle={t('subcontractorPage.subtitle')}
      />
      <SubcontractorStats overview={overview} />
      <SubcontractorLists overview={overview} />
      <Title order={2}>{t('subcontractorPage.balanceByYarn')}</Title>
      <QueryState query={overview.balances}>
        {(view) => <BalanceMatrix view={view} isLinked={false} />}
      </QueryState>
    </Stack>
  );
}
