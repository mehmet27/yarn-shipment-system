import type { AuditRecordView } from '@iplik/api-client/audit';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Badge, Group, Paper, Stack, Text } from '@iplik/ui';
import { AuditActor } from './AuditActor';
import { useActionLabel, useEntityLabel, useServiceLabel } from './auditText';

/** Dar ekranda denetim kayıtları: tablo yerine her kayıt bir kart. */
export function AuditCards({
  records,
  names,
}: {
  records: AuditRecordView[];
  names?: ReadonlyMap<string, string> | undefined;
}) {
  const { t } = useTranslation();
  const actionLabel = useActionLabel();
  const entityLabel = useEntityLabel();
  const serviceLabel = useServiceLabel();
  return (
    <Stack gap="sm">
      {records.map((record) => (
        <Paper key={record.id} withBorder p="md">
          <Stack gap={4}>
            <Group justify="space-between" wrap="nowrap" align="flex-start">
              <Text fw={600} title={record.action}>
                {actionLabel(record.action)}
              </Text>
              {record.decision === 'DENIED' ? <Badge color="red">{t('audit.denied')}</Badge> : null}
            </Group>
            <Text size="sm" c="dimmed">
              {formatDateTime(record.occurredAt)}
            </Text>
            <Text size="sm">
              <AuditActor actor={record.actor} names={names} /> ·{' '}
              <span title={record.entity?.id}>{entityLabel(record.entity)}</span>
            </Text>
            <Text size="xs" c="dimmed" title={record.sourceService}>
              {t('audit.col.source')}: {serviceLabel(record.sourceService)}
            </Text>
          </Stack>
        </Paper>
      ))}
    </Stack>
  );
}
