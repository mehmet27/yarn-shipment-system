import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Modal, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useVoidBags } from '../hooks/useVoidBags';
import { bagErrorKey } from './bagErrors';

export type VoidReasonModalProps = {
  isOpen: boolean;
  uniqueKeys: string[];
  onClose: () => void;
  onVoided: () => void;
};

/** İptal gerekçesini ister ve onaydan sonra çuvalları iptal eder. */
export function VoidReasonModal({ isOpen, uniqueKeys, onClose, onVoided }: VoidReasonModalProps) {
  const { t } = useTranslation();
  const [reason, setReason] = useState('');
  const mutation = useVoidBags();
  const close = () => {
    setReason('');
    mutation.reset();
    onClose();
  };
  const confirm = () => {
    mutation.mutate(
      { uniqueKeys, reason: reason.trim() },
      {
        onSuccess: () => {
          close();
          onVoided();
        },
      },
    );
  };
  return (
    <Modal opened={isOpen} onClose={close} title={t('bag.void.title')}>
      <Stack>
        <Text>{t('bag.void.warning', { count: uniqueKeys.length })}</Text>
        <TextInput
          label={t('bag.void.reason')}
          value={reason}
          onChange={(event) => {
            setReason(event.currentTarget.value);
          }}
          required
        />
        {mutation.isError && (
          <Alert color="red" role="alert">
            {t(bagErrorKey(mutation.error))}
          </Alert>
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={close}>
            {t('bag.void.cancel')}
          </Button>
          <Button
            color="red"
            onClick={confirm}
            loading={mutation.isPending}
            disabled={reason.trim() === ''}
          >
            {t('bag.void.confirm', { count: uniqueKeys.length })}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
