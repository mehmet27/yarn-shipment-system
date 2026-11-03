import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Group } from '@iplik/ui';

export type StatusBadgesProps = { account: LoginAccountView };

/** Etkin / devre dışı ve ilk girişini bekleyen hesap. */
export function StatusBadges({ account }: StatusBadgesProps) {
  const { t } = useTranslation();
  return (
    <Group gap={4}>
      <Badge color={account.enabled ? 'green' : 'gray'}>
        {t(account.enabled ? 'users.status.enabled' : 'users.status.disabled')}
      </Badge>
      {account.passwordChangeRequired && (
        <Badge color="yellow" variant="light">
          {t('users.status.passwordChangeRequired')}
        </Badge>
      )}
    </Group>
  );
}
