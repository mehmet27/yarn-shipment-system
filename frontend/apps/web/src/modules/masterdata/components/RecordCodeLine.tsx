import { useTranslation } from '@iplik/i18n';
import { Badge, Group, Text } from '@iplik/ui';
import type { ManagedKind, RecordRow } from '../kinds';

/** Kodlu türlerde kod ve iplik numarası ya da etiket rengi (renk örneğiyle). */
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
      {kind === 'qualityGrade' && record.extra !== undefined && (
        <Badge color={record.extra} aria-hidden>
          {record.code}
        </Badge>
      )}
    </Group>
  );
}
