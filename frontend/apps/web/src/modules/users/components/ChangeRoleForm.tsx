import type { LoginAccountView, StaffRole, WorkArea } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { useChangeAccountRole } from '../hooks/useAccountMutations';
import { asStaffRole, needsWorkArea } from '../staffRoles';
import { StaffRoleSelect } from './StaffRoleSelect';
import { WorkAreaSelect } from './WorkAreaSelect';

const KNOWN_CODES = [
  'LOGIN_ACCOUNT_NOT_FOUND',
  'LOGIN_ACCOUNT_ROLE_LOCKED',
  'LOGIN_ACCOUNT_UNAVAILABLE',
  'INVALID_MASTER_DATA',
];

function errorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `users.changeRole.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

/** Yeni rolü seçtirir ve kaydeder; hata pencerede kalır, başarıda `onClose` çağrılır. */
export function ChangeRoleForm({
  account,
  onClose,
}: {
  account: LoginAccountView;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const [role, setRole] = useState<StaffRole | undefined>(asStaffRole(account.roles[0] ?? null));
  const [workArea, setWorkArea] = useState<WorkArea | undefined>(account.workArea);
  const mutation = useChangeAccountRole(account.id);
  const isAreaMissing = needsWorkArea(role) && workArea === undefined;
  return (
    <Stack>
      <Text size="sm">{t('users.changeRole.intro', { username: account.username })}</Text>
      <StaffRoleSelect value={role} onChange={setRole} />
      {needsWorkArea(role) && <WorkAreaSelect value={workArea} onChange={setWorkArea} />}
      <Text size="xs" c="dimmed">
        {t('users.changeRole.reloginHint')}
      </Text>
      {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
      <Button
        loading={mutation.isPending}
        disabled={isAreaMissing}
        onClick={() => {
          if (role === undefined) return;
          const request =
            needsWorkArea(role) && workArea !== undefined ? { role, workArea } : { role };
          mutation.mutate(request, { onSuccess: onClose });
        }}
      >
        {t('users.changeRole.submit')}
      </Button>
    </Stack>
  );
}
