import { useTranslation } from '@iplik/i18n';
import { Autocomplete, SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { MasterSelect } from '@/modules/shipment';
import { useLotChoice } from '../hooks/useLotChoice';
import type { useReceiptSubmit } from '../useReceiptSubmit';

type ReceiptState = ReturnType<typeof useReceiptSubmit>;

const STATE_TEXT = {
  existing: { key: 'stock.receipt.lotExisting', color: 'dimmed' },
  new: { key: 'stock.receipt.lotNew', color: 'dimmed' },
  conflict: { key: 'stock.receipt.lotConflict', color: 'red' },
} as const;

/** Parti no (listeden seçilir ya da yeni yazılır), irsaliye no ve (parti yeniyse) kalite sınıfı. */
export function ReceiptLotFields({
  draft,
  setDraft,
  field,
}: Pick<ReceiptState, 'draft' | 'setDraft' | 'field'>) {
  const { t } = useTranslation();
  const { suggestions, state } = useLotChoice(draft);
  const note = state === 'none' || state === 'unknown' ? undefined : STATE_TEXT[state];
  return (
    <>
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <Stack gap={4}>
          <Autocomplete
            label={t('stock.receipt.lotNo')}
            data={suggestions}
            value={draft.lotNo}
            onChange={(lotNo) => {
              setDraft((current) => ({ ...current, lotNo }));
            }}
            comboboxProps={{ withinPortal: true, hideDetached: false }}
          />
          {note !== undefined && (
            <Text size="xs" c={note.color}>
              {t(note.key)}
            </Text>
          )}
        </Stack>
        <TextInput label={t('stock.receipt.waybillNo')} {...field('waybillNo')} />
      </SimpleGrid>
      {(state === 'new' || state === 'unknown') && (
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
