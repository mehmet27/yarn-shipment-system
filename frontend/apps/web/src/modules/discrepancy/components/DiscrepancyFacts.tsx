import type { DiscrepancyView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Paper, Stack } from '@iplik/ui';
import { Fact } from './Fact';
import { formatDifference } from './formatDifference';

/** Kuralın ürettiği beklenen/gerçek değerler ve fark; çözülmüşse karar bilgisi. */
export function DiscrepancyFacts({ discrepancy: d }: { discrepancy: DiscrepancyView }) {
  const { t } = useTranslation();
  return (
    <Paper withBorder p="md">
      <Stack gap="sm">
        <Fact label={t('discrepancy.facts.type')} value={t(`discrepancy.type.${d.type}`)} />
        <Fact label={t('discrepancy.facts.rule')} value={d.ruleCode} />
        <Fact label={t('discrepancy.facts.expected')} value={d.expectedValue} />
        <Fact label={t('discrepancy.facts.actual')} value={d.actualValue} />
        <Fact
          label={t('discrepancy.facts.difference')}
          value={formatDifference(d.diffKg, d.diffPct)}
        />
        <Fact
          label={t('discrepancy.facts.due')}
          value={d.dueAt === undefined ? undefined : formatDateTime(d.dueAt)}
        />
        <Fact
          label={t('discrepancy.facts.resolution')}
          value={
            d.resolution === undefined ? undefined : t(`discrepancy.resolution.${d.resolution}`)
          }
        />
        <Fact label={t('discrepancy.facts.note')} value={d.resolutionNote} />
        <Fact label={t('discrepancy.facts.liableParty')} value={d.liableParty} />
      </Stack>
    </Paper>
  );
}
