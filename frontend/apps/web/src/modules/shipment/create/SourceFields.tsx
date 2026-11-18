import { useTranslation } from '@iplik/i18n';
import { SegmentedControl, Stack, Text, TextInput } from '@iplik/ui';
import type { CreateDraft, CreateFieldErrors, SourceKind } from './createDraft';
import { MasterSelect } from './MasterSelect';

type SourceFieldsProps = {
  draft: CreateDraft;
  onChange: (draft: CreateDraft) => void;
  errors: CreateFieldErrors;
};

const SOURCE_KINDS: readonly SourceKind[] = ['CENTRAL', 'SUBCONTRACTOR', 'DIRECT'];

/** İpliğin nereden çıktığı tek seçimle belirlenir; yalnız seçilen kaynağın alanları açılır. */
export function SourceFields({ draft, onChange, errors }: SourceFieldsProps) {
  const { t } = useTranslation();
  const sourceError = errors.sourceSubcontractor;
  const directError = errors.directTransfer;
  return (
    <Stack gap="sm">
      <SegmentedControl
        fullWidth
        styles={{ label: { whiteSpace: 'normal', textAlign: 'center' } }}
        aria-label={t('create.source.title')}
        data={SOURCE_KINDS.map((kind) => ({ value: kind, label: t(`create.source.${kind}`) }))}
        value={draft.sourceKind}
        onChange={(value) => {
          const sourceKind = SOURCE_KINDS.find((kind) => kind === value);
          if (sourceKind !== undefined) onChange({ ...draft, sourceKind });
        }}
      />
      {draft.sourceKind === 'SUBCONTRACTOR' && (
        <MasterSelect
          kind="subcontractor"
          label={t('create.sourceSubcontractor')}
          value={draft.sourceSubcontractor}
          isRequired
          {...(sourceError === undefined ? {} : { error: t(`create.errors.${sourceError}`) })}
          onChange={(sourceSubcontractor) => {
            onChange({ ...draft, sourceSubcontractor });
          }}
        />
      )}
      {draft.sourceKind === 'DIRECT' && (
        <Stack gap="xs">
          <MasterSelect
            kind="supplier"
            label={t('create.directTransferSupplier')}
            value={draft.directTransferSupplier}
            isRequired
            {...(directError === undefined ? {} : { error: t(`create.errors.${directError}`) })}
            onChange={(directTransferSupplier) => {
              onChange({ ...draft, directTransferSupplier });
            }}
          />
          <Text size="sm" c="dimmed">
            {t('create.directTransferHint')}
          </Text>
          <TextInput
            label={t('create.directTransferWaybillNo')}
            value={draft.directTransferWaybillNo}
            maxLength={100}
            onChange={(event) => {
              onChange({ ...draft, directTransferWaybillNo: event.currentTarget.value });
            }}
          />
        </Stack>
      )}
    </Stack>
  );
}
