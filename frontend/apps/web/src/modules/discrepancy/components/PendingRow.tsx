import type { DiscrepancyView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Anchor, Box, Group, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { toSeverityColor } from './discrepancyColor';

const DOT_SIZE_PX = 8;

/** Sade iş satırı: solda önem noktası, kayıt no, soluk tür; sağda son tarih. */
export function PendingRow({ item, isFirst }: { item: DiscrepancyView; isFirst: boolean }) {
  const { t } = useTranslation();
  return (
    <Group
      justify="space-between"
      wrap="nowrap"
      gap="sm"
      px="md"
      py="xs"
      style={{ borderTop: isFirst ? undefined : '1px solid var(--app-line-soft)' }}
    >
      <Group gap="sm" wrap="nowrap" miw={0}>
        <Box
          aria-hidden
          w={DOT_SIZE_PX}
          h={DOT_SIZE_PX}
          bg={`var(--mantine-color-${toSeverityColor(item.severity)}-filled)`}
          style={{ borderRadius: '50%', flexShrink: 0 }}
        />
        <Anchor
          component={Link}
          to={`/panel/uyusmazliklar/${item.id}`}
          fw={600}
          c="var(--app-text)"
          style={{ whiteSpace: 'nowrap' }}
        >
          {item.discrepancyNo}
        </Anchor>
        <Text size="sm" c="dimmed" truncate>
          {t(`discrepancy.type.${item.type}`)}
        </Text>
      </Group>
      {item.dueAt !== undefined && (
        <Text size="sm" c="dimmed" style={{ whiteSpace: 'nowrap' }}>
          {t('discrepancy.pending.due', { date: formatDateTime(item.dueAt) })}
        </Text>
      )}
    </Group>
  );
}
