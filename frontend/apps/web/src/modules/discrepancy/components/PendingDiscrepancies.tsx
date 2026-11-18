import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Anchor, Group, Paper, Stack, Text, Title } from '@iplik/ui';
import { Link } from 'react-router';
import { usePendingDiscrepancies } from '../hooks/usePendingDiscrepancies';

const SHOWN = 5;

/**
 * Kalitenin ana sayfadaki iş listesi: karar bekleyen uyuşmazlıklar. Yalnızca karar verme yetkisi olan kullanıcıya
 * görünür; bekleyen yoksa hiçbir şey çizmez.
 */
export function PendingDiscrepancies() {
  const { t } = useTranslation();
  const pending = usePendingDiscrepancies();
  if (pending.length === 0) return null;
  return (
    <Paper withBorder p="md" data-testid="pending-discrepancies">
      <Stack gap="xs">
        <Title order={2} size="h4">
          {t('discrepancy.pending.title', { count: pending.length })}
        </Title>
        {pending.slice(0, SHOWN).map((item) => (
          <Group key={item.id} justify="space-between">
            <Anchor component={Link} to={`/panel/uyusmazliklar/${item.id}`}>
              {item.discrepancyNo}
            </Anchor>
            {item.dueAt !== undefined && (
              <Text size="sm" c="dimmed">
                {t('discrepancy.pending.due', { date: formatDateTime(item.dueAt) })}
              </Text>
            )}
          </Group>
        ))}
        <Anchor component={Link} to="/panel/uyusmazliklar" size="sm">
          {t('discrepancy.pending.all')}
        </Anchor>
      </Stack>
    </Paper>
  );
}
