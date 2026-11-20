import type { AuditRecordView } from '@iplik/api-client/audit';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Paper, Table } from '@iplik/ui';
import { useWideLayout } from '@/modules/shared';
import { DotBadge } from '@iplik/ui';
import { useUserNames } from '../hooks/useUserNames';
import { AuditActor } from './AuditActor';
import { AuditCards } from './AuditCards';
import { useActionLabel, useEntityLabel, useServiceLabel } from './auditText';

/** Denetim kayıtları: geniş ekranda tablo, dar ekranda kartlar. */
export function AuditTable({ records }: { records: AuditRecordView[] }) {
  const { t } = useTranslation();
  const actionLabel = useActionLabel();
  const entityLabel = useEntityLabel();
  const serviceLabel = useServiceLabel();
  const names = useUserNames(
    records.flatMap((record) => (record.actor.userId === undefined ? [] : [record.actor.userId])),
  ).data;
  const isDesktop = useWideLayout();
  if (!isDesktop) return <AuditCards records={records} names={names} />;
  return (
    <Paper withBorder radius="md" style={{ overflow: 'hidden' }}>
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
                <Table.Td style={{ whiteSpace: 'nowrap' }}>
                  {formatDateTime(record.occurredAt)}
                </Table.Td>
                <Table.Td>
                  <AuditActor actor={record.actor} names={names} />
                </Table.Td>
                <Table.Td>
                  <span title={record.action}>{actionLabel(record.action)}</span>{' '}
                  {record.decision === 'DENIED' ? (
                    <DotBadge color="red">{t('audit.denied')}</DotBadge>
                  ) : null}
                </Table.Td>
                <Table.Td>
                  <span title={record.entity?.id}>{entityLabel(record.entity)}</span>
                </Table.Td>
                <Table.Td>
                  <span title={record.sourceService}>{serviceLabel(record.sourceService)}</span>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  );
}
