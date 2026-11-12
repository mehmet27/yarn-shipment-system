import type { LoginAccountView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Anchor, Badge, Group, Modal, Stack, Text, useDisclosure } from '@iplik/ui';
import { useCurrentUser } from '@/modules/auth';
import { hasChangeableRole } from '../staffRoles';
import { ChangeNameForm } from './ChangeNameForm';

/** Kimlik: ad soyad başta, giriş adı altında; kendi hesap "Siz" rozetiyle, açılış tarihi imleç üstünde görünür. */
export function AccountIdentity({ account }: { account: LoginAccountView }) {
  const { t } = useTranslation();
  const { data: me } = useCurrentUser();
  const [isAdding, adding] = useDisclosure(false);
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
        <>
          <Text size="xs" c="dimmed">
            {t('users.noName')}
          </Text>
          {hasChangeableRole(account) && (
            <>
              <Anchor component="button" type="button" size="xs" onClick={adding.open}>
                {t('users.addName')}
              </Anchor>
              <Modal opened={isAdding} onClose={adding.close} title={t('users.changeName.title')}>
                <ChangeNameForm account={account} onClose={adding.close} />
              </Modal>
            </>
          )}
        </>
      ) : (
        <Text size="xs" c="dimmed" style={{ overflowWrap: 'anywhere' }}>
          {account.username}
        </Text>
      )}
    </Stack>
  );
}
