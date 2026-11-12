import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Group } from '@iplik/ui';

export type RoleBadgesProps = { account: LoginAccountView };

/** Hesabın rolleri; kalite kullanıcısında çalışma alanı da gösterilir. */
export function RoleBadges({ account }: RoleBadgesProps) {
  const { t } = useTranslation();
  return (
    <Group gap={4}>
      {account.roles.map((role) => (
        <Badge key={role} variant="light">
          {t(`users.role.${role}`)}
        </Badge>
      ))}
      {account.workArea !== undefined && (
        <Badge variant="outline" color="gray">
          {t(`users.workArea.${account.workArea}`)}
        </Badge>
      )}
    </Group>
  );
}
