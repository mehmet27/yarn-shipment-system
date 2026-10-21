import { useTranslation } from '@iplik/i18n';
import { Stack } from '@iplik/ui';
import type { CreateDraft } from './createDraft';
import { withSubcontractor } from './createDraft';
import { MasterSelect } from './MasterSelect';

export type CreateFieldsProps = { draft: CreateDraft; onChange: (draft: CreateDraft) => void };

/** Dört seçici tek sütunda; şoför listesi seçili fasoncuya göre daralır. */
export function CreateFields({ draft, onChange }: CreateFieldsProps) {
  const { t } = useTranslation();
  return (
    <Stack>
      <MasterSelect
        kind="site"
        label={t('create.site')}
        value={draft.site}
        isRequired
        onChange={(site) => {
          onChange({ ...draft, site });
        }}
      />
      <MasterSelect
        kind="subcontractor"
        label={t('create.subcontractor')}
        value={draft.subcontractor}
        isRequired
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
    </Stack>
  );
}
