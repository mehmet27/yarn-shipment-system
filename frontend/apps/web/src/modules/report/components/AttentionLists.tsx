import type { DashboardView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Anchor, Group, SectionCard, Stack, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { CardBleed } from './CardBleed';
import { DotBadge } from '@iplik/ui';

/** Vadesi geçen uyuşmazlıklar (ön analiz Bölüm 13): müdürün kararını bekleyen iş, sayfanın en üstünde durur. */
export function AttentionLists({ dashboard }: { dashboard: DashboardView }) {
  const { t } = useTranslation();
  const items = dashboard.overdueDiscrepancies;
  return (
    <SectionCard
      title={t('report.dashboard.overdueList')}
      icon="discrepancies"
      tone={items.length === 0 ? 'brand' : 'danger'}
    >
      {items.length === 0 ? (
        <Text c="dimmed">{t('report.dashboard.noOverdue')}</Text>
      ) : (
        <CardBleed>
          <Stack gap={0}>
            {items.map((item, index) => (
              <Group
                key={item.discrepancyId}
                justify="space-between"
                px="md"
                py="xs"
                style={{ borderTop: index === 0 ? undefined : '1px solid var(--app-line-soft)' }}
              >
                <Anchor
                  component={Link}
                  to={`/panel/uyusmazliklar/${item.discrepancyId}`}
                  c="var(--app-text)"
                >
                  {item.discrepancyNo}
                </Anchor>
                <Group gap="xs">
                  <DotBadge color={item.severity === 'RED' ? 'red' : 'yellow'}>
                    {t(`discrepancy.severity.${item.severity}`)}
                  </DotBadge>
                  <Text size="sm" c="dimmed">
                    {t('report.dashboard.dueAt', { at: formatDateTime(item.dueAt) })}
                  </Text>
                </Group>
              </Group>
            ))}
          </Stack>
        </CardBleed>
      )}
    </SectionCard>
  );
}
