import type { StaffRole } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Select } from '@iplik/ui';
import { STAFF_ROLES, asStaffRole } from '../staffRoles';

export type StaffRoleSelectProps = {
  value: StaffRole | undefined;
  onChange: (role: StaffRole | undefined) => void;
  error?: string | undefined;
};

/** Fabrika içi rol seçimi; şoför ve fasoncu seçeneği yoktur. */
export function StaffRoleSelect({ value, onChange, error }: StaffRoleSelectProps) {
  const { t } = useTranslation();
  return (
    <Select
      label={t('users.field.roles')}
      data={STAFF_ROLES.map((role) => ({ value: role, label: t(`users.role.${role}`) }))}
      value={value ?? null}
      onChange={(next) => {
        onChange(asStaffRole(next));
      }}
      allowDeselect={false}
      withAsterisk
      error={error}
    />
  );
}
