import { useTranslation } from '@iplik/i18n';
import { Table } from '@iplik/ui';
import type { ManagedKind, RecordRow } from '../kinds';
import { RecordTableRow } from './RecordTableRow';

export type RecordTableProps = {
  kind: ManagedKind;
  rows: readonly RecordRow[];
  onEdit: (record: RecordRow) => void;
};

/** Tanım listesi tablo olarak: ad, ayrıntı, durum ve ikonlu eylemler; dar ekranda yatay kayar. */
export function RecordTable({ kind, rows, onEdit }: RecordTableProps) {
  const { t } = useTranslation();
  return (
    <Table.ScrollContainer minWidth={560}>
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
  );
}
