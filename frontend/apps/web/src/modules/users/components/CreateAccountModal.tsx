import { useTranslation } from '@iplik/i18n';
import { Modal } from '@iplik/ui';
import { CreateAccountForm } from './CreateAccountForm';

export type CreateAccountModalProps = { opened: boolean; onClose: () => void };

/** Hesap açma penceresi; kapanınca içerik sökülür, form her açılışta temiz başlar. */
export function CreateAccountModal({ opened, onClose }: CreateAccountModalProps) {
  const { t } = useTranslation();
  return (
    <Modal opened={opened} onClose={onClose} title={t('users.create.title')}>
      <CreateAccountForm onClose={onClose} />
    </Modal>
  );
}
