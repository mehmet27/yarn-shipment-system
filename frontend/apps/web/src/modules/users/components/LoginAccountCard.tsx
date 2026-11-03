import type { LoginAccountView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Group, Paper, Stack, Text } from '@iplik/ui';
import { RoleBadges } from './RoleBadges';
import { StatusBadges } from './StatusBadges';

export type LoginAccountCardProps = { account: LoginAccountView };

/** Dar ekranda tek kullanıcı: kullanıcı adı başta, ad, e-posta, rol ve durum altında. */
export function LoginAccountCard({ account }: LoginAccountCardProps) {
  const { t } = useTranslation();
  return (
    <Paper withBorder p="md" role="group" aria-label={account.username}>
      <Stack gap="xs">
        <Group justify="space-between" wrap="nowrap" align="flex-start">
          <Text fw={600} style={{ overflowWrap: 'anywhere' }}>
            {account.username}
          </Text>
          <StatusBadges account={account} />
        </Group>
        <Text size="sm" {...(account.fullName === undefined ? { c: 'dimmed' } : {})}>
          {account.fullName ?? t('users.noName')}
        </Text>
        {account.email !== undefined && (
          <Text size="sm" c="dimmed" style={{ overflowWrap: 'anywhere' }}>
            {account.email}
          </Text>
        )}
        <RoleBadges account={account} />
        {account.createdAt !== undefined && (
          <Text size="xs" c="dimmed">
            {t('users.field.createdAt')}: {formatDateTime(account.createdAt)}
          </Text>
        )}
      </Stack>
    </Paper>
  );
}
