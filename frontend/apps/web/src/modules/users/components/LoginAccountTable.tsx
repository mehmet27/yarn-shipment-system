import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Paper, Stack, Table } from '@iplik/ui';
import { useWideLayout } from '@/modules/shared';
import { LoginAccountCard } from './LoginAccountCard';
import { LoginAccountRow } from './LoginAccountRow';

const TABLE_MIN_WIDTH = 640;

export type LoginAccountTableProps = { accounts: readonly LoginAccountView[] };

/** Mobil öncelikli (FE-UI-07): dar ekranda kartlar, geniş ekranda tablo. Şifre sütunu yoktur. */
export function LoginAccountTable({ accounts }: LoginAccountTableProps) {
  const { t } = useTranslation();
  const isDesktop = useWideLayout();
  if (!isDesktop) {
    return (
      <Stack gap="sm">
        {accounts.map((account) => (
          <LoginAccountCard key={account.id} account={account} />
        ))}
      </Stack>
    );
  }
  return (
    <Paper withBorder radius="md" style={{ overflow: 'hidden' }}>
      <Table.ScrollContainer minWidth={TABLE_MIN_WIDTH}>
        <Table highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('users.field.user')}</Table.Th>
              <Table.Th>{t('users.field.roles')}</Table.Th>
              <Table.Th>{t('users.field.status')}</Table.Th>
              <Table.Th ta="right">{t('users.field.actions')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {accounts.map((account) => (
              <LoginAccountRow key={account.id} account={account} />
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Paper>
  );
}
