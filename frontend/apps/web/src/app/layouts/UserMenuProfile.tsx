import type { CurrentUser } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Group, Stack, Text } from '@iplik/ui';

export type UserMenuProfileProps = { user: CurrentUser };

/** Menünün üstündeki kart: ad, roller ve iki adımlı doğrulama durumu. */
export function UserMenuProfile({ user }: UserMenuProfileProps) {
  const { t } = useTranslation();
  return (
    <Stack gap={6} px="sm" py="xs">
      <Text fw={700}>{user.fullName}</Text>
      <Group gap={4}>
        {user.roles.map((role) => (
          <Badge key={role} variant="light" size="sm">
            {t(`users.role.${role}`)}
          </Badge>
        ))}
      </Group>
      {user.mfaVerified ? (
        <Text size="xs" c="green.8">
          {t('app.mfaOn')}
        </Text>
      ) : null}
    </Stack>
  );
}
