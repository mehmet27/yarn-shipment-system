import type { LotView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Modal, Stack, TextInput, useMediaQuery } from '@iplik/ui';
import { useLotDraft } from '../hooks/useLotDraft';
import { useCatalogChoices } from '../hooks/useLots';
import { useLotSiteChoices } from '../hooks/useLotSiteChoices';
import { errorOf, LotChoiceField } from './LotChoiceField';

export type LotFormProps = {
  /** Düzenlenen kilitsiz parti; yeni partide yok. */
  lot: LotView | undefined;
  onClose: () => void;
  onSaved: (isNew: boolean) => void;
};

/** Parti ekle/düzenle; lokasyon yalnızca eklerken seçilir. Dar ekranda tam ekran açılır. */
export function LotForm({ lot, onClose, onSaved }: LotFormProps) {
  const { t } = useTranslation();
  const isNarrow = useMediaQuery('(max-width: 47.99em)', false);
  const draft = useLotDraft(lot, onSaved);
  const sites = useLotSiteChoices();
  const catalog = useCatalogChoices();
  const formError = errorOf(draft.errors, 'form', t);
  return (
    <Modal
      opened
      onClose={onClose}
      fullScreen={isNarrow}
      closeButtonProps={{ 'aria-label': t('common.close') }}
      title={t(lot === undefined ? 'lots.add' : 'lots.edit')}
    >
      <Stack>
        <LotChoiceField field="siteId" data={sites} draft={draft} isDisabled={lot !== undefined} />
        <TextInput
          label={t('lots.fields.lotNo')}
          value={draft.draft.lotNo}
          onChange={(event) => {
            draft.setField('lotNo', event.currentTarget.value);
          }}
          error={errorOf(draft.errors, 'lotNo', t)}
          withAsterisk
        />
        <LotChoiceField field="productId" data={catalog.data?.products ?? []} draft={draft} />
        <LotChoiceField field="qualityGradeId" data={catalog.data?.grades ?? []} draft={draft} />
        <TextInput
          type="date"
          label={t('lots.fields.productionDate')}
          value={draft.draft.productionDate}
          onChange={(event) => {
            draft.setField('productionDate', event.currentTarget.value);
          }}
          error={errorOf(draft.errors, 'productionDate', t)}
          withAsterisk
        />
        {formError !== undefined && <Alert color="red">{formError}</Alert>}
        <Button loading={draft.isPending} onClick={draft.submit}>
          {t('masterData.save')}
        </Button>
      </Stack>
    </Modal>
  );
}
