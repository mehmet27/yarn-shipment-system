import type { AuditEntity, AuditRecordView } from '@iplik/api-client/audit';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Badge, Table } from '@iplik/ui';

const SHORT_ID_LENGTH = 8;

function entityLabel(entity: AuditEntity | undefined): string {
  if (entity === undefined) return '—';
  return entity.id === undefined
    ? entity.type
    : `${entity.type} ${entity.id.slice(0, SHORT_ID_LENGTH)}`;
}

/** Denetim kayıtlarını tablo olarak gösterir; dar ekranda yatay kaydırılır. */
export function AuditTable({ records }: { records: AuditRecordView[] }) {
  const { t } = useTranslation();
  return (
    <Table.ScrollContainer minWidth={640}>
      <Table>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('audit.col.time')}</Table.Th>
            <Table.Th>{t('audit.col.actor')}</Table.Th>
            <Table.Th>{t('audit.col.action')}</Table.Th>
            <Table.Th>{t('audit.col.entity')}</Table.Th>
            <Table.Th>{t('audit.col.source')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {records.map((record) => (
            <Table.Tr key={record.id}>
              <Table.Td>{formatDateTime(record.occurredAt)}</Table.Td>
              <Table.Td>
                {record.actor.role ?? record.actor.clientId ?? record.actor.userId ?? '—'}
              </Table.Td>
              <Table.Td>
                {record.action}{' '}
                {record.decision === 'DENIED' ? (
                  <Badge color="red">{t('audit.denied')}</Badge>
                ) : null}
              </Table.Td>
              <Table.Td>{entityLabel(record.entity)}</Table.Td>
              <Table.Td>{record.sourceService}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
