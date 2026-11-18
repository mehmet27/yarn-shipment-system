import type { Permission } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Checkbox, Group, SectionCard, Stack } from '@iplik/ui';
import type { PermissionGroup } from '../groups';

export type PermissionGroupCardProps = {
  readonly group: PermissionGroup;
  readonly selected: ReadonlySet<Permission>;
  readonly lockedPermissions: readonly Permission[];
  readonly isEditable: boolean;
  readonly onToggle: (permission: Permission, checked: boolean) => void;
};

/** Bir ekran grubunun izinleri; yalnızca yöneticide olan izinler (ve yönetici rolünün tümü) kilitlidir. */
export function PermissionGroupCard({
  group,
  selected,
  lockedPermissions,
  isEditable,
  onToggle,
}: PermissionGroupCardProps) {
  const { t } = useTranslation();
  const countLabel = `${String(group.permissions.filter((p) => selected.has(p)).length)} / ${String(group.permissions.length)}`;
  return (
    <SectionCard
      title={t(`permissions.groups.${group.key}`)}
      actions={
        <Badge variant="light" color="gray" aria-label={countLabel} style={{ flexShrink: 0 }}>
          {countLabel}
        </Badge>
      }
    >
      <Stack gap="xs">
        {group.permissions.map((permission) => {
          const isLocked = lockedPermissions.includes(permission);
          return (
            <Group key={permission} justify="space-between" wrap="nowrap">
              <Checkbox
                label={t(`permissions.items.${permission}`)}
                checked={selected.has(permission)}
                disabled={!isEditable || isLocked}
                onChange={(event) => {
                  onToggle(permission, event.currentTarget.checked);
                }}
              />
              {isLocked && isEditable && (
                <Badge variant="outline" color="gray" style={{ flexShrink: 0 }}>
                  {t('permissions.locked')}
                </Badge>
              )}
            </Group>
          );
        })}
      </Stack>
    </SectionCard>
  );
}
