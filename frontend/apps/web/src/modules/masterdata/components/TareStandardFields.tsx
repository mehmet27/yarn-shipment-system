import type { PackagingType } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Select, SimpleGrid, TextInput } from '@iplik/ui';
import { useSiteChoices } from '../hooks/useSiteChoices';
import type { TareStandardDraft, TareStandardErrors } from '../tareStandardDraft';
import { PACKAGING_TYPES } from '../tareStandardDraft';

function toPackagingType(value: string | null): PackagingType {
  return PACKAGING_TYPES.find((type) => type === value) ?? 'BAG';
}

type Props = {
  draft: TareStandardDraft;
  errors: TareStandardErrors;
  onChange: (patch: Partial<TareStandardDraft>) => void;
};

export function TareStandardFields({ draft, errors, onChange }: Props) {
  const { t } = useTranslation();
  const sites = useSiteChoices();
  return (
    <SimpleGrid cols={{ base: 1, sm: 2 }}>
      <Select
        label={t('masterData.tareStandard.site')}
        withAsterisk
        data={(sites.data ?? []).map((site) => ({ value: site.id, label: site.name }))}
        value={draft.siteId === '' ? null : draft.siteId}
        onChange={(value) => {
          onChange({ siteId: value ?? '' });
        }}
        error={errors.siteId ? t('masterData.tareStandard.invalidSite') : undefined}
      />
      <Select
        label={t('masterData.tareStandard.packagingType')}
        withAsterisk
        allowDeselect={false}
        data={PACKAGING_TYPES.map((type) => ({
          value: type,
          label: t(`masterData.tareStandard.types.${type}`),
        }))}
        value={draft.packagingType}
        onChange={(value) => {
          onChange({ packagingType: toPackagingType(value) });
        }}
      />
      <TextInput
        label={t('masterData.tareStandard.standardTareKg')}
        withAsterisk
        inputMode="decimal"
        value={draft.standardTareKg}
        onChange={(event) => {
          onChange({ standardTareKg: event.currentTarget.value });
        }}
        error={errors.standardTareKg ? t('masterData.tareStandard.invalidKg') : undefined}
      />
      <TextInput
        label={t('masterData.tareStandard.maxDeviationPct')}
        withAsterisk
        inputMode="decimal"
        value={draft.maxDeviationPct}
        onChange={(event) => {
          onChange({ maxDeviationPct: event.currentTarget.value });
        }}
        error={errors.maxDeviationPct ? t('masterData.tareStandard.invalidPct') : undefined}
      />
    </SimpleGrid>
  );
}
