import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Group } from '@iplik/ui';

export type StatusBadgesProps = { account: LoginAccountView };

/** Etkin / devre dışı ve ilk girişini bekleyen hesap. */
export function StatusBadges({ account }: StatusBadgesProps) {
  const { t } = useTranslation();
  return (
    <Group gap={4}>
      <Badge
        color={account.enabled ? 'green' : 'gray'}
        variant="light"
        style={{ whiteSpace: 'nowrap' }}
      >
        {t(account.enabled ? 'users.status.enabled' : 'users.status.disabled')}
      </Badge>
      {account.passwordRequestedAt !== undefined && (
        <Badge color="red" variant="light" style={{ whiteSpace: 'nowrap' }}>
          {t('users.status.passwordRequested')}
        </Badge>
      )}
      {account.passwordChangeRequired && (
        <Badge color="yellow" variant="light" style={{ whiteSpace: 'nowrap' }}>
          {t('users.status.passwordChangeRequired')}
        </Badge>
      )}
    </Group>
  );
}
