import { useTranslation } from '@iplik/i18n';
import { Paper, Stack, Table } from '@iplik/ui';
import { useWideLayout } from '@/modules/shared';
import type { ManagedKind, RecordRow } from '../kinds';
import { RecordCard } from './RecordCard';
import { RecordTableRow } from './RecordTableRow';

const TABLE_MIN_WIDTH = 640;

export type RecordTableProps = {
  kind: ManagedKind;
  rows: readonly RecordRow[];
  onEdit: (record: RecordRow) => void;
};

/** Tanım listesi: geniş ekranda kartın içinde tablo, dar ekranda her kayıt bir kart. */
export function RecordTable({ kind, rows, onEdit }: RecordTableProps) {
  const { t } = useTranslation();
  const isDesktop = useWideLayout();
  if (!isDesktop) {
    return (
      <Stack gap="sm">
        {rows.map((record) => (
          <RecordCard key={record.id} kind={kind} record={record} onEdit={onEdit} />
        ))}
      </Stack>
    );
  }
  return (
    <Paper withBorder radius="md" style={{ overflow: 'hidden' }}>
      <Table.ScrollContainer minWidth={TABLE_MIN_WIDTH}>
        <Table verticalSpacing="xs" aria-label={t(`masterData.kinds.${kind}.tab`)}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('masterData.columns.name')}</Table.Th>
              <Table.Th>{t('masterData.columns.detail')}</Table.Th>
              <Table.Th>{t('masterData.columns.status')}</Table.Th>
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rows.map((record) => (
              <RecordTableRow key={record.id} kind={kind} record={record} onEdit={onEdit} />
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  );
}
