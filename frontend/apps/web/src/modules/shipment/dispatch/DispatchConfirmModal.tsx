import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Icon, Modal, Stack, Text } from '@iplik/ui';
import type { Kilogram, WeighingComparison } from '@iplik/units';
import { formatKg, formatKgText } from '@iplik/units';

export type DispatchConfirmModalProps = {
  isOpen: boolean;
  isPending: boolean;
  /** Sunucu reddederse gösterilecek hazır metin. */
  failure: string | undefined;
  plannedNetKg: string | undefined;
  netKg: Kilogram | undefined;
  comparison: WeighingComparison | undefined;
  onConfirm: () => void;
  onClose: () => void;
};

/**
 * "İleri" sonrası onay: tartı özetlenir; fark toleransı aşıyorsa onaylanırsa müdüre kantar uyuşmazlığı açılacağı
 * açıkça söylenir. Depo bu adımı onaylamadan hiçbir şey kaydedilmez, sevkiyat yola çıkmaz.
 */
export function DispatchConfirmModal({
  isOpen,
  isPending,
  failure,
  plannedNetKg,
  netKg,
  comparison,
  onConfirm,
  onClose,
}: DispatchConfirmModalProps) {
  const { t } = useTranslation();
  const isMismatch = comparison?.exceedsTolerance === true;
  return (
    <Modal opened={isOpen} onClose={onClose} title={t('dispatch.confirm.title')}>
      <Stack>
        <Text>{t('dispatch.confirm.leaves')}</Text>
        {plannedNetKg !== undefined && (
          <Text size="sm">
            {t('dispatch.confirm.expected', { kg: formatKgText(plannedNetKg) })}
          </Text>
        )}
        {netKg !== undefined && (
          <Text size="sm">{t('dispatch.confirm.measured', { kg: formatKg(netKg) })}</Text>
        )}
        {isMismatch && (
          <Alert color="orange" icon={<Icon name="discrepancies" />}>
            {t('dispatch.confirm.mismatch', { diff: formatKgText(comparison.diffKg) })}
          </Alert>
        )}
        {failure !== undefined && <Alert color="red">{failure}</Alert>}
        <Group justify="flex-end">
          <Button variant="default" leftSection={<Icon name="arrowLeft" />} onClick={onClose}>
            {t('dispatch.confirm.back')}
          </Button>
          <Button
            loading={isPending}
            leftSection={<Icon name="truck" />}
            {...(isMismatch ? { color: 'orange' } : {})}
            onClick={onConfirm}
          >
            {t(isMismatch ? 'dispatch.confirm.openMismatch' : 'dispatch.confirm.dispatch')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
