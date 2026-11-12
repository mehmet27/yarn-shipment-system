import type { LoginAccountView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Badge, Group, Stack, Text } from '@iplik/ui';
import { useCurrentUser } from '@/modules/auth';

/** Kimlik: ad soyad başta, giriş adı altında; kendi hesap "Siz" rozetiyle, açılış tarihi imleç üstünde görünür. */
export function AccountIdentity({ account }: { account: LoginAccountView }) {
  const { t } = useTranslation();
  const { data: me } = useCurrentUser();
  const title =
    account.createdAt === undefined
      ? undefined
      : `${t('users.field.createdAt')}: ${formatDateTime(account.createdAt)}`;
  return (
    <Stack gap={0} {...(title === undefined ? {} : { title })}>
      <Group gap={6} wrap="nowrap">
        <Text fw={600} size="sm" style={{ overflowWrap: 'anywhere' }}>
          {account.fullName ?? account.username}
        </Text>
        {me?.userId === account.id && (
          <Badge size="sm" variant="light" color="gray">
            {t('users.you')}
          </Badge>
        )}
      </Group>
      {account.fullName === undefined ? (
        <Text size="xs" c="dimmed">
          {t('users.noName')}
        </Text>
      ) : (
        <Text size="xs" c="dimmed" style={{ overflowWrap: 'anywhere' }}>
          {account.username}
        </Text>
      )}
    </Stack>
  );
}
