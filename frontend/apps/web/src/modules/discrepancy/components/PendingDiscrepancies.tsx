import { useTranslation } from '@iplik/i18n';
import { Anchor, Box, SectionCard, Stack } from '@iplik/ui';
import { Link } from 'react-router';
import { usePendingDiscrepancies } from '../hooks/usePendingDiscrepancies';
import { PendingRow } from './PendingRow';

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
    <SectionCard
      title={t('discrepancy.pending.title', { count: pending.length })}
      icon="discrepancies"
      testId="pending-discrepancies"
    >
      <Stack gap={0} m="calc(var(--mantine-spacing-md) * -1)">
        {pending.slice(0, SHOWN).map((item, index) => (
          <PendingRow key={item.id} item={item} isFirst={index === 0} />
        ))}
        <Box px="md" py="xs" style={{ borderTop: '1px solid var(--app-line-soft)' }}>
          <Anchor component={Link} to="/panel/uyusmazliklar" size="sm">
            {t('discrepancy.pending.all')}
          </Anchor>
        </Box>
      </Stack>
    </SectionCard>
  );
}
