import { useTranslation } from '@iplik/i18n';
import { Fragment } from 'react';
import { SimpleGrid } from '@iplik/ui';
import type { CreateDraft, CreateFieldErrors } from './createDraft';
import { withSubcontractor } from './createDraft';
import { MasterSelect } from './MasterSelect';
import { OrderMetaFields } from './OrderMetaFields';

type PartiesFieldsProps = {
  draft: CreateDraft;
  onChange: (draft: CreateDraft) => void;
  errors: CreateFieldErrors;
};

/** Taraflar: alıcı fasoncu, şoför, araç; altında yalnız sipariş formu PDF'inde basılan sipariş no ve yetkili kişi. */
export function PartiesFields({ draft, onChange, errors }: PartiesFieldsProps) {
  const { t } = useTranslation();
  return (
    <Fragment>
      <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="sm">
        <MasterSelect
          kind="subcontractor"
          label={t('create.subcontractor')}
          value={draft.subcontractor}
          isRequired
          {...(errors.subcontractor === undefined
            ? {}
            : { error: t(`create.errors.${errors.subcontractor}`) })}
          onChange={(subcontractor) => {
            onChange(withSubcontractor(draft, subcontractor));
          }}
        />
        <MasterSelect
          kind="driver"
          label={t('create.driver')}
          value={draft.driver}
          {...(draft.subcontractor === undefined
            ? {}
            : { subcontractorId: draft.subcontractor.value })}
          onChange={(driver) => {
            onChange({ ...draft, driver });
          }}
        />
        <MasterSelect
          kind="vehicle"
          label={t('create.vehicle')}
          value={draft.vehicle}
          onChange={(vehicle) => {
            onChange({ ...draft, vehicle });
          }}
        />
      </SimpleGrid>
      <OrderMetaFields draft={draft} onChange={onChange} errors={errors} />
    </Fragment>
  );
}
