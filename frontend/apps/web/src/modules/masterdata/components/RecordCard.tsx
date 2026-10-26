import { useTranslation } from '@iplik/i18n';
import { Badge, Button, Group, Paper, Stack, Text } from '@iplik/ui';
import { useSetActive } from '../hooks/useSetActive';
import type { ManagedKind, RecordRow } from '../kinds';
import { LoginAccountButton } from './LoginAccountButton';

export type RecordCardProps = {
  kind: ManagedKind;
  record: RecordRow;
  onEdit: (record: RecordRow) => void;
};

/** Mobilde tablo yerine kart: ad, durum rozeti ve iki büyük eylem düğmesi. */
export function RecordCard({ kind, record, onEdit }: RecordCardProps) {
  const { t } = useTranslation();
  const mutation = useSetActive(kind);
  return (
    <Paper withBorder p="md" role="group" aria-label={record.label}>
      <Stack gap="sm">
        <Group justify="space-between" wrap="nowrap">
          <Text fw={600} {...(record.isActive ? {} : { c: 'dimmed' })}>
            {record.label}
          </Text>
          {!record.isActive && <Badge color="gray">{t('masterData.inactive')}</Badge>}
        </Group>
        {kind === 'driver' && (
          <Text size="sm" c="dimmed">
            {record.ownerTitle === undefined
              ? t('masterData.noOwner')
              : t('masterData.ownerOf', { title: record.ownerTitle })}
          </Text>
        )}
        <LoginAccountButton kind={kind} record={record} />
        <Group grow>
          <Button
            variant="default"
            onClick={() => {
              onEdit(record);
            }}
          >
            {t('masterData.edit')}
          </Button>
          <Button
            variant="light"
            color={record.isActive ? 'red' : 'green'}
            loading={mutation.isPending}
            onClick={() => {
              mutation.mutate({ id: record.id, isActive: !record.isActive });
            }}
          >
            {t(record.isActive ? 'masterData.deactivate' : 'masterData.activate')}
          </Button>
        </Group>
      </Stack>
    </Paper>
  );
}
