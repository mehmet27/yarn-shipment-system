import type { LoginAccountView, StaffRole } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { useChangeAccountRole } from '../hooks/useStaffAccountMutations';
import { asStaffRole } from '../staffRoles';
import { StaffRoleSelect } from './StaffRoleSelect';

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
  const mutation = useChangeAccountRole(account.id);
  return (
    <Stack>
      <Text size="sm">{t('users.changeRole.intro', { username: account.username })}</Text>
      <StaffRoleSelect value={role} onChange={setRole} />
      {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
      <Button
        loading={mutation.isPending}
        onClick={() => {
          if (role === undefined) return;
          mutation.mutate({ role }, { onSuccess: onClose });
        }}
      >
        {t('users.changeRole.submit')}
      </Button>
    </Stack>
  );
}
