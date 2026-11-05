import type { AuditRecordView } from '@iplik/api-client/audit';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Badge, Table, useMediaQuery } from '@iplik/ui';
import { AuditActor } from './AuditActor';
import { AuditCards } from './AuditCards';
import { useActionLabel, useEntityLabel } from './auditText';

/** Denetim kayıtları: geniş ekranda tablo, dar ekranda kartlar. */
export function AuditTable({ records }: { records: AuditRecordView[] }) {
  const { t } = useTranslation();
  const actionLabel = useActionLabel();
  const entityLabel = useEntityLabel();
  const isDesktop = useMediaQuery('(min-width: 48em)', true, { getInitialValueInEffect: false });
  if (!isDesktop) return <AuditCards records={records} />;
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
                <AuditActor actor={record.actor} />
              </Table.Td>
              <Table.Td>
                <span title={record.action}>{actionLabel(record.action)}</span>{' '}
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
