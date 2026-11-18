import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Icon, Stack, Text } from '@iplik/ui';
import { PhotoFailedNotice } from './PhotoFailedNotice';
import { BagMismatchModal } from './BagMismatchModal';
import { DispatchConfirmModal } from './DispatchConfirmModal';
import { DispatchFields } from './DispatchFields';
import { failureKey } from './dispatchSubmit';
import { BAGS_TAB_QUERY } from '../components/ShipmentFileTabs';
import { useDispatchFlow } from './useDispatchFlow';

export type DispatchFormProps = {
  shipmentId: string;
  /** Beklenen net (kg metni); varsa "İleri"de tartıyla karşılaştırılıp onay istenir. */
  plannedNetKg?: string;
  onSaved: () => void;
};

/** Depo çıkış tartısı: kantar fişi tek formda; net yalnızca brüt ve daradan hesaplanır. */
export function DispatchForm({ shipmentId, plannedNetKg, onSaved }: DispatchFormProps) {
  const { t } = useTranslation();
  const flow = useDispatchFlow(shipmentId, plannedNetKg, onSaved);
  const failure = flow.mutation.isError ? t(failureKey(flow.mutation.error)) : undefined;
  return (
    <Stack>
      <Text size="sm" c="dimmed">
        {t('dispatch.hint')}
      </Text>
      <Text size="sm" c="dimmed">
        {t('dispatch.mismatchNotice')}
      </Text>
      <DispatchFields
        draft={flow.draft}
        onChange={flow.change}
        weighing={flow.weighing}
        isReasonMissing={flow.isReasonMissing}
        isShort={flow.comparison?.isShort === true}
      />
      {flow.error !== undefined && <Alert color="red">{t(`dispatch.errors.${flow.error}`)}</Alert>}
      {!flow.isConfirming && failure !== undefined && <Alert color="red">{failure}</Alert>}
      {flow.isPhotoMissing ? (
        <PhotoFailedNotice onContinue={onSaved} />
      ) : (
        <Button rightSection={<Icon name="arrowRight" />} onClick={flow.review}>
          {t('dispatch.next')}
        </Button>
      )}
      <BagMismatchModal
        mismatch={flow.bagMismatch.mismatch}
        bagsHref={`/panel/sevkiyat/${shipmentId}?${BAGS_TAB_QUERY}`}
        onClose={flow.bagMismatch.dismiss}
      />
      <DispatchConfirmModal
        isOpen={flow.isConfirming}
        isPending={flow.mutation.isPending}
        failure={failure}
        plannedNetKg={plannedNetKg}
        netKg={flow.weighing?.netKg}
        comparison={flow.comparison}
        onConfirm={flow.confirm}
        onClose={flow.closeConfirm}
      />
    </Stack>
  );
}
