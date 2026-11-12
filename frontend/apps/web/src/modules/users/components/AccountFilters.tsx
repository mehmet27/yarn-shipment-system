import { useTranslation } from '@iplik/i18n';
import { Group, SegmentedControl, Select } from '@iplik/ui';
import { type AccountFilter, type StatusFilter } from '../accountFilter';
import { STAFF_ROLES } from '../staffRoles';

export type AccountFiltersProps = {
  value: AccountFilter;
  onChange: (value: AccountFilter) => void;
};

const STATUSES: readonly StatusFilter[] = ['ALL', 'ENABLED', 'DISABLED', 'PENDING'];
const STATUS_KEYS: Record<StatusFilter, string> = {
  ALL: 'users.filter.allStatuses',
  ENABLED: 'users.status.enabled',
  DISABLED: 'users.status.disabled',
  PENDING: 'users.status.passwordChangeRequired',
};

/** Durum ve rol süzgeçleri. */
export function AccountFilters({ value, onChange }: AccountFiltersProps) {
  const { t } = useTranslation();
  return (
    <Group gap="sm">
      <SegmentedControl
        aria-label={t('users.filter.status')}
        value={value.status}
        data={STATUSES.map((status) => ({ value: status, label: t(STATUS_KEYS[status]) }))}
        onChange={(status) => {
          const next = STATUSES.find((candidate) => candidate === status);
          if (next !== undefined) onChange({ ...value, status: next });
        }}
      />
      <Select
        aria-label={t('users.filter.role')}
        placeholder={t('users.filter.allRoles')}
        clearable
        value={value.role}
        data={STAFF_ROLES.map((role) => ({ value: role, label: t(`users.role.${role}`) }))}
        onChange={(role) => {
          onChange({ ...value, role });
        }}
      />
    </Group>
  );
}
