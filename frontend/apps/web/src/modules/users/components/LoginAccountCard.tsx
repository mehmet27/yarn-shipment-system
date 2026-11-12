import type { LoginAccountView } from '@iplik/api-client';
import { Group, Paper, Stack } from '@iplik/ui';
import { AccountActions } from './AccountActions';
import { AccountIdentity } from './AccountIdentity';
import { RoleBadges } from './RoleBadges';
import { StatusBadges } from './StatusBadges';

export type LoginAccountCardProps = { account: LoginAccountView };

/** Dar ekranda tek kullanıcı: kimlik ve durum üstte, rol ve işlemler altında. */
export function LoginAccountCard({ account }: LoginAccountCardProps) {
  return (
    <Paper
      withBorder
      p="md"
      role="group"
      aria-label={account.username}
      {...(account.enabled ? {} : { style: { opacity: 0.65 } })}
    >
      <Stack gap="xs">
        <Group justify="space-between" wrap="nowrap" align="flex-start">
          <AccountIdentity account={account} />
          <StatusBadges account={account} />
        </Group>
        <RoleBadges account={account} />
        <AccountActions account={account} />
      </Stack>
    </Paper>
  );
}
