import { useTranslation } from '@iplik/i18n';
import { Badge, Button, Group, Icon, Table, Text } from '@iplik/ui';
import { useDeactivateGuard } from '../hooks/useDeactivateGuard';
import type { ManagedKind, RecordRow } from '../kinds';
import { BalanceWarningModal } from './BalanceWarningModal';
import { RecordCodeLine } from './RecordCodeLine';

export type RecordTableRowProps = {
  kind: ManagedKind;
  record: RecordRow;
  onEdit: (record: RecordRow) => void;
};

/** Tablo satırı: ad, ayrıntı (kod / bağlı fasoncu), durum rozeti ve ikonlu iki eylem düğmesi. */
export function RecordTableRow({ kind, record, onEdit }: RecordTableRowProps) {
  const { t } = useTranslation();
  const guard = useDeactivateGuard(kind);
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
      <Table.Td>
        <Badge color={record.isActive ? 'green' : 'gray'} variant="light">
          {t(record.isActive ? 'masterData.active' : 'masterData.inactive')}
        </Badge>
      </Table.Td>
      <Table.Td>
        <Group gap="xs" wrap="nowrap" justify="flex-end">
          <Button
            variant="default"
           
            leftSection={<Icon name="edit" size={16} />}
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
            leftSection={<Icon name={record.isActive ? 'stop' : 'check'} size={16} />}
            onClick={() => {
              void guard.request(record);
            }}
          >
            {t(record.isActive ? 'masterData.deactivate' : 'masterData.activate')}
          </Button>
        </Group>
        <BalanceWarningModal
          warning={guard.warning}
          onConfirm={guard.confirm}
          onClose={guard.dismiss}
        />
      </Table.Td>
    </Table.Tr>
  );
}
