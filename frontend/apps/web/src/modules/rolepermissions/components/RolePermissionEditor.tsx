import type { Permission } from '@iplik/api-client';
import type { RolePermissionRow } from '@iplik/api-client/role-permissions';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Stack, Text } from '@iplik/ui';
import { PERMISSION_GROUPS } from '../groups';
import { useRoleDraft } from '../hooks/useRoleDraft';
import { PermissionGroupCard } from './PermissionGroupCard';

export type RolePermissionEditorProps = {
  readonly row: RolePermissionRow;
  readonly lockedPermissions: readonly Permission[];
};

/** Tek rolün izinleri: ekrana göre gruplu onay kutuları. Yönetici rolü yalnızca gösterilir. */
export function RolePermissionEditor({ row, lockedPermissions }: RolePermissionEditorProps) {
  const { t } = useTranslation();
  const draft = useRoleDraft(row);
  return (
    <Stack>
      {!row.editable && <Alert color="gray">{t('permissions.fixedRole')}</Alert>}
      {PERMISSION_GROUPS.map((group) => (
        <PermissionGroupCard
          key={group.key}
          group={group}
          selected={draft.selected}
          lockedPermissions={lockedPermissions}
          isEditable={row.editable}
          onToggle={draft.toggle}
        />
      ))}
      {(draft.save.isError || draft.reset.isError) && (
        <Alert color="red">{t('permissions.failed')}</Alert>
      )}
      {draft.notice === 'saved' && <Alert color="green">{t('permissions.saved')}</Alert>}
      {draft.notice === 'reset' && <Alert color="green">{t('permissions.resetDone')}</Alert>}
      {row.editable && (
        <Group>
          <Button loading={draft.save.isPending} disabled={!draft.isDirty} onClick={draft.submit}>
            {t('permissions.save')}
          </Button>
          <Button
            variant="default"
            loading={draft.reset.isPending}
            disabled={draft.isDefault}
            onClick={draft.restore}
          >
            {t('permissions.reset')}
          </Button>
          {draft.isDirty && <Text c="dimmed">{t('permissions.changed')}</Text>}
        </Group>
      )}
    </Stack>
  );
}
