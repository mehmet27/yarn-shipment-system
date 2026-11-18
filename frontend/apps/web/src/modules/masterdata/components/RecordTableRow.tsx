import { useTranslation } from '@iplik/i18n';
import { Table, Text } from '@iplik/ui';
import { DotBadge } from '@iplik/ui';
import type { ManagedKind, RecordRow } from '../kinds';
import { RecordCodeLine } from './RecordCodeLine';
import { RecordRowActions } from './RecordRowActions';

export type RecordTableRowProps = {
  kind: ManagedKind;
  record: RecordRow;
  onEdit: (record: RecordRow) => void;
};

/** Tablo satırı: ad, ayrıntı (kod / bağlı fasoncu), durum rozeti ve sağa yaslı eylemler. */
export function RecordTableRow({ kind, record, onEdit }: RecordTableRowProps) {
  const { t } = useTranslation();
  return (
    <Table.Tr aria-label={record.label}>
      <Table.Td>
        <Text fw={600} {...(record.isActive ? {} : { c: 'dimmed' })}>
          {record.label}
        </Text>
      </Table.Td>
      <Table.Td>
        {record.code !== undefined && <RecordCodeLine kind={kind} record={record} />}
        {kind === 'driver' && (
          <Text size="sm" c="dimmed">
            {record.ownerTitle === undefined
              ? t('masterData.noOwner')
              : t('masterData.ownerOf', { title: record.ownerTitle })}
          </Text>
        )}
      </Table.Td>
      <Table.Td style={{ whiteSpace: 'nowrap' }}>
        <DotBadge color={record.isActive ? 'green' : 'gray'}>
          {t(record.isActive ? 'masterData.active' : 'masterData.inactive')}
        </DotBadge>
      </Table.Td>
      <Table.Td>
        <RecordRowActions kind={kind} record={record} onEdit={onEdit} />
      </Table.Td>
    </Table.Tr>
  );
}
