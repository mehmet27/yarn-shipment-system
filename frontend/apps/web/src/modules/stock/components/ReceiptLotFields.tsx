import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, TextInput } from '@iplik/ui';
import { MasterSelect } from '@/modules/shipment';
import type { useReceiptSubmit } from '../useReceiptSubmit';

type ReceiptState = ReturnType<typeof useReceiptSubmit>;

/** Parti no, irsaliye no ve (parti yazılmışsa) kalite sınıfı. */
export function ReceiptLotFields({
  draft,
  setDraft,
  field,
}: Pick<ReceiptState, 'draft' | 'setDraft' | 'field'>) {
  const { t } = useTranslation();
  return (
    <>
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <TextInput label={t('stock.receipt.lotNo')} {...field('lotNo')} />
        <TextInput label={t('stock.receipt.waybillNo')} {...field('waybillNo')} />
      </SimpleGrid>
      {draft.lotNo.trim() !== '' && (
        <MasterSelect
          kind="qualityGrade"
          label={t('stock.receipt.qualityGrade')}
          value={draft.qualityGrade}
          onChange={(qualityGrade) => {
            setDraft((current) => ({ ...current, qualityGrade }));
          }}
        />
      )}
    </>
  );
}
