import type { Permission } from '@iplik/api-client';
import type { RolePermissionRow } from '@iplik/api-client/role-permissions';
import { useState } from 'react';
import { useRolePermissionMutations } from './useRolePermissionMutations';

function sameSet(a: ReadonlySet<Permission>, b: readonly Permission[]): boolean {
  return a.size === b.length && b.every((permission) => a.has(permission));
}

/** Bir rolün düzenleme taslağı: seçili izinler, kaydet ve varsayılana dön. */
export function useRoleDraft(row: RolePermissionRow) {
  const { save, reset } = useRolePermissionMutations();
  const [selected, setSelected] = useState<ReadonlySet<Permission>>(new Set(row.permissions));
  const [notice, setNotice] = useState<'saved' | 'reset' | undefined>(undefined);
  const isDirty = !sameSet(selected, row.permissions);
  const defaultSet = new Set(row.defaults);
  const diffCount =
    [...selected].filter((p) => !defaultSet.has(p)).length +
    row.defaults.filter((p) => !selected.has(p)).length;
  const isDefault = !isDirty && sameSet(new Set(row.defaults), row.permissions);

  const toggle = (permission: Permission, checked: boolean) => {
    const next = new Set(selected);
    if (checked) next.add(permission);
    else next.delete(permission);
    setSelected(next);
    setNotice(undefined);
  };
  const submit = () => {
    save.mutate(
      { role: row.role, request: { permissions: [...selected] } },
      {
        onSuccess: () => {
          setNotice('saved');
        },
      },
    );
  };
  const restore = () => {
    reset.mutate(row.role, {
      onSuccess: (view) => {
        const fresh = view.roles.find((item) => item.role === row.role);
        setSelected(new Set(fresh?.permissions ?? []));
        setNotice('reset');
      },
    });
  };
  return { selected, notice, isDirty, isDefault, diffCount, toggle, submit, restore, save, reset };
}
