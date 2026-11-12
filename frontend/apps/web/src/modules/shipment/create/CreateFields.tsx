import { useTranslation } from '@iplik/i18n';
import { Paper, SimpleGrid, Stack } from '@iplik/ui';
import type { CreateDraft, CreateFieldErrors } from './createDraft';
import { withSubcontractor } from './createDraft';
import { MasterSelect } from './MasterSelect';
import { RecipeLines } from './RecipeLines';
import { Section } from './Section';
import { SourceFields } from './SourceFields';

export type CreateFieldsProps = {
  draft: CreateDraft;
  onChange: (draft: CreateDraft) => void;
  errors?: CreateFieldErrors;
};

/** Üç kart: taraflar (alıcı, şoför, araç), iplik kaynağı ve reçete satırları. */
export function CreateFields({ draft, onChange, errors = {} }: CreateFieldsProps) {
  const { t } = useTranslation();
  return (
    <Stack>
      <Section title={t('create.sections.parties')}>
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
      </Section>
      <Section title={t('create.source.title')}>
        <SourceFields draft={draft} onChange={onChange} errors={errors} />
      </Section>
      <Paper withBorder p="md" radius="md">
        <RecipeLines
          lines={draft.lines}
          showErrors={errors.lines !== undefined}
          onChange={(lines) => {
            onChange({ ...draft, lines });
          }}
        />
      </Paper>
    </Stack>
  );
}
