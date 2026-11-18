import { useTranslation } from '@iplik/i18n';
import { Button, Group, Icon, Modal, Stack } from '@iplik/ui';
import { Link } from 'react-router';
import { BagCountMessage } from './BagCountMessage';
import { BagWeightMessage } from './BagWeightMessage';
import type { BagMismatch } from './useBagMismatch';

export type BagMismatchModalProps = {
  mismatch: BagMismatch | undefined;
  /** Çuvalların eklendiği sevkiyat dosyası sekmesi. */
  bagsHref: string;
  onClose: () => void;
};

/**
 * Önce çuval sayısı, sonra çuval kiloları denetlenir; tutmuyorsa iki değer gösterilir, sevkiyat yola çıkmaz. Depo çuvalları
 * kontrol etmek için çuval ekleme ekranına gider ya da tartı değerlerini düzeltmek için geri döner.
 */
export function BagMismatchModal({ mismatch, bagsHref, onClose }: BagMismatchModalProps) {
  const { t } = useTranslation();
  return (
    <Modal
      opened={mismatch !== undefined}
      onClose={onClose}
      title={
        mismatch?.kind === 'count'
          ? t('dispatch.bagMismatch.countTitle')
          : t('dispatch.bagMismatch.title')
      }
    >
      <Stack>
        {mismatch?.kind === 'count' && (
          <BagCountMessage enteredCount={mismatch.enteredCount} bagCount={mismatch.bagCount} />
        )}
        {mismatch?.kind === 'weight' && (
          <BagWeightMessage
            bagKg={mismatch.bagKg}
            measuredKg={mismatch.measuredKg}
            diffKg={mismatch.diffKg}
          />
        )}
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            {t('dispatch.bagMismatch.fixWeighing')}
          </Button>
          <Button component={Link} to={bagsHref} leftSection={<Icon name="list" />}>
            {t('dispatch.bagMismatch.checkBags')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
