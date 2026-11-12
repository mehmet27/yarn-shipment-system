import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, Modal, useDisclosure } from '@iplik/ui';
import { ChangeNameForm } from './ChangeNameForm';

/** Fabrika içi hesabın ad soyadını düzenler; giriş adı, rol ve oturumlar değişmez. */
export function ChangeNameButton({ account }: { account: LoginAccountView }) {
  const { t } = useTranslation();
  const [isOpen, { open, close }] = useDisclosure(false);
  return (
    <>
      <Button
        variant="default"
        onClick={open}
        aria-label={t('users.changeName.open', { username: account.username })}
      >
        {t('users.changeName.action')}
      </Button>
      <Modal opened={isOpen} onClose={close} title={t('users.changeName.title')}>
        <ChangeNameForm account={account} onClose={close} />
      </Modal>
    </>
  );
}
