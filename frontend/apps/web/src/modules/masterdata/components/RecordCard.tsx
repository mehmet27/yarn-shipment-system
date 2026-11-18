import { useTranslation } from '@iplik/i18n';
import { Group, Paper, Stack, Text } from '@iplik/ui';
import type { ManagedKind, RecordRow } from '../kinds';
import { DotBadge } from '@iplik/ui';
import { RecordCodeLine } from './RecordCodeLine';
import { RecordRowActions } from './RecordRowActions';

export type RecordCardProps = {
  kind: ManagedKind;
  record: RecordRow;
  onEdit: (record: RecordRow) => void;
};

/** Dar ekranda tek kayıt: ad ve durum üstte, ayrıntı altında, eylemler en altta. */
export function RecordCard({ kind, record, onEdit }: RecordCardProps) {
  const { t } = useTranslation();
  return (
    <Paper withBorder radius="md" p="md" role="group" aria-label={record.label}>
      <Stack gap="xs">
        <Group justify="space-between" wrap="nowrap" align="flex-start">
          <Text
            fw={600}
            style={{ overflowWrap: 'anywhere' }}
            {...(record.isActive ? {} : { c: 'dimmed' })}
          >
            {record.label}
          </Text>
          <DotBadge color={record.isActive ? 'green' : 'gray'}>
            {t(record.isActive ? 'masterData.active' : 'masterData.inactive')}
          </DotBadge>
        </Group>
        {record.code !== undefined && <RecordCodeLine kind={kind} record={record} />}
        {kind === 'driver' && (
          <Text size="sm" c="dimmed">
            {record.ownerTitle === undefined
              ? t('masterData.noOwner')
              : t('masterData.ownerOf', { title: record.ownerTitle })}
          </Text>
        )}
        <RecordRowActions kind={kind} record={record} onEdit={onEdit} />
      </Stack>
    </Paper>
  );
}
