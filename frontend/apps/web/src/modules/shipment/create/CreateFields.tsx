import { useTranslation } from '@iplik/i18n';
import { Stack } from '@iplik/ui';
import type { CreateDraft, CreateFieldErrors } from './createDraft';
import { RecipeLines } from './RecipeLines';
import { PartiesFields } from './PartiesFields';
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
      <Section title={t('create.sections.parties')} icon="user" step={1}>
        <PartiesFields draft={draft} onChange={onChange} errors={errors} />
      </Section>
      <Section title={t('create.source.title')} icon="lots" step={2}>
        <SourceFields draft={draft} onChange={onChange} errors={errors} />
      </Section>
      <Section title={t('create.recipe.title')} icon="list" step={3}>
        <RecipeLines
          lines={draft.lines}
          showErrors={errors.lines !== undefined}
          onChange={(lines) => {
            onChange({ ...draft, lines });
          }}
        />
      </Section>
    </Stack>
  );
}
