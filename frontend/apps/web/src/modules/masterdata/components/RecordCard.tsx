import { useTranslation } from '@iplik/i18n';
import { Badge, Button, Group, Paper, Stack, Text } from '@iplik/ui';
import { useDeactivateGuard } from '../hooks/useDeactivateGuard';
import type { ManagedKind, RecordRow } from '../kinds';
import { BalanceWarningModal } from './BalanceWarningModal';
import { RecordCodeLine } from './RecordCodeLine';

export type RecordCardProps = {
  kind: ManagedKind;
  record: RecordRow;
  onEdit: (record: RecordRow) => void;
};

/** Mobilde tablo yerine kart: ad, durum rozeti ve iki büyük eylem düğmesi. */
export function RecordCard({ kind, record, onEdit }: RecordCardProps) {
  const { t } = useTranslation();
  const guard = useDeactivateGuard(kind);
  return (
    <Paper withBorder p="md" role="group" aria-label={record.label}>
      <Stack gap="sm">
        <Group justify="space-between" wrap="nowrap">
          <Text fw={600} {...(record.isActive ? {} : { c: 'dimmed' })}>
            {record.label}
          </Text>
          {!record.isActive && <Badge color="gray">{t('masterData.inactive')}</Badge>}
        </Group>
        {record.code !== undefined && <RecordCodeLine kind={kind} record={record} />}
        {kind === 'driver' && (
          <Text size="sm" c="dimmed">
            {record.ownerTitle === undefined
              ? t('masterData.noOwner')
              : t('masterData.ownerOf', { title: record.ownerTitle })}
          </Text>
        )}
        <Group grow maw={{ base: '100%', sm: 360 }}>
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
            loading={guard.isBusy}
            onClick={() => {
              void guard.request(record);
            }}
          >
            {t(record.isActive ? 'masterData.deactivate' : 'masterData.activate')}
          </Button>
        </Group>
      </Stack>
      <BalanceWarningModal
        warning={guard.warning}
        onConfirm={guard.confirm}
        onClose={guard.dismiss}
      />
    </Paper>
  );
}
