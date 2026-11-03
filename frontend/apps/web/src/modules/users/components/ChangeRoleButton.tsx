import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, Modal, useDisclosure } from '@iplik/ui';
import { ChangeRoleForm } from './ChangeRoleForm';

/** Fabrika içi hesabın rolünü değiştirir; kişinin açık oturumları kapanır ve yeni rolle girer. */
export function ChangeRoleButton({ account }: { account: LoginAccountView }) {
  const { t } = useTranslation();
  const [isOpen, { open, close }] = useDisclosure(false);
  return (
    <>
      <Button
        variant="default"
        onClick={open}
        aria-label={t('users.changeRole.open', { username: account.username })}
      >
        {t('users.changeRole.action')}
      </Button>
      <Modal opened={isOpen} onClose={close} title={t('users.changeRole.title')}>
        <ChangeRoleForm account={account} onClose={close} />
      </Modal>
    </>
  );
}
