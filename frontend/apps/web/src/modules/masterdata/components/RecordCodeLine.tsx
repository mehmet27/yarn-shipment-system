import { useTranslation } from '@iplik/i18n';
import { Badge, Group, Text } from '@iplik/ui';
import type { ManagedKind, RecordRow } from '../kinds';

/** Kodlu türlerde kod, iplik numarası ya da kalite sınıfı sırası ve sınıfın kullanıldığı parti sayısı. */
export function RecordCodeLine({ kind, record }: { kind: ManagedKind; record: RecordRow }) {
  const { t } = useTranslation();
  return (
    <Group gap="xs">
      <Badge variant="outline" color="gray">
        {record.code}
      </Badge>
      {record.extra !== undefined && (
        <Text size="sm" c="dimmed">
          {t(`masterData.kinds.${kind}.extra`)}: {record.extra}
        </Text>
      )}
      {kind === 'qualityGrade' && record.lotCount !== undefined && (
        <Text size="sm" c="dimmed">
          {t('masterData.kinds.qualityGrade.lotCount', { count: record.lotCount })}
        </Text>
      )}
    </Group>
  );
}
