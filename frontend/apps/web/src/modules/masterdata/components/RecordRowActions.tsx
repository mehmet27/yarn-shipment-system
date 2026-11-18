import { useTranslation } from '@iplik/i18n';
import { Button, Group, Icon } from '@iplik/ui';
import { useDeactivateGuard } from '../hooks/useDeactivateGuard';
import type { ManagedKind, RecordRow } from '../kinds';
import { BalanceWarningModal } from './BalanceWarningModal';

export type RecordRowActionsProps = {
  kind: ManagedKind;
  record: RecordRow;
  onEdit: (record: RecordRow) => void;
};

/** Satır eylemleri: "Düzenle" kenarlı, pasife alma/açma sakin (çerçevesiz); ikisi aynı hizada, satır atlamaz. */
export function RecordRowActions({ kind, record, onEdit }: RecordRowActionsProps) {
  const { t } = useTranslation();
  const guard = useDeactivateGuard(kind);
  return (
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
        variant="subtle"
        color={record.isActive ? 'gray' : 'green'}
        loading={guard.isBusy}
        leftSection={<Icon name={record.isActive ? 'stop' : 'check'} size={16} />}
        onClick={() => {
          void guard.request(record);
        }}
      >
        {t(record.isActive ? 'masterData.deactivate' : 'masterData.activate')}
      </Button>
      <BalanceWarningModal
        warning={guard.warning}
        onConfirm={guard.confirm}
        onClose={guard.dismiss}
      />
    </Group>
  );
}
