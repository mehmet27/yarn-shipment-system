import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Icon, Modal, Stack, Text } from '@iplik/ui';
import { formatKgText } from '@iplik/units';
import { Link } from 'react-router';
import type { BagMismatch } from './useBagMismatch';

export type BagMismatchModalProps = {
  mismatch: BagMismatch | undefined;
  /** Çuvalların eklendiği sevkiyat dosyası sekmesi. */
  bagsHref: string;
  onClose: () => void;
};

/**
 * Çuvalların kilo toplamı araç net kilosunu tutmuyor: iki kilo gösterilir, sevkiyat yola çıkmaz. Depo çuvalları
 * kontrol etmek için çuval ekleme ekranına gider ya da tartı değerlerini düzeltmek için geri döner.
 */
export function BagMismatchModal({ mismatch, bagsHref, onClose }: BagMismatchModalProps) {
  const { t } = useTranslation();
  return (
    <Modal
      opened={mismatch !== undefined}
      onClose={onClose}
      title={t('dispatch.bagMismatch.title')}
    >
      <Stack>
        {mismatch !== undefined && (
          <Text>
            {t('dispatch.bagMismatch.body', {
              bagKg: formatKgText(mismatch.bagKg),
              measuredKg: formatKgText(mismatch.measuredKg),
            })}
          </Text>
        )}
        {mismatch !== undefined && (
          <Alert color="red" icon={<Icon name="discrepancies" />}>
            {t('dispatch.bagMismatch.blocked', { diff: formatKgText(mismatch.diffKg) })}
          </Alert>
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
