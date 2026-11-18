import { useTranslation } from '@iplik/i18n';
import { Divider, Stack } from '@iplik/ui';
import type { CreateDraft, CreateFieldErrors } from './createDraft';
import { FireRateField } from './FireRateField';
import { RecipeLines } from './RecipeLines';
import { PartiesFields } from './PartiesFields';
import { Section } from './Section';
import { SourceFields } from './SourceFields';

export type CreateFieldsProps = {
  draft: CreateDraft;
  onChange: (draft: CreateDraft) => void;
  errors?: CreateFieldErrors;
  /** Ayarlardaki varsayılan fire oranı (%); taslakta girilmemişse bu gösterilir. */
  defaultFirePct: string;
};

/** Üç kart: taraflar (alıcı, şoför, araç), iplik kaynağı; reçete satırları ve fire oranı. */
export function CreateFields({ draft, onChange, errors = {}, defaultFirePct }: CreateFieldsProps) {
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
        <Stack gap="md">
          <RecipeLines
            lines={draft.lines}
            showErrors={errors.lines !== undefined}
            onChange={(lines) => {
              onChange({ ...draft, lines });
            }}
          />
          <Divider color="var(--app-line)" />
          <FireRateField
            value={draft.fireRatePct ?? defaultFirePct}
            errorCode={errors.fireRate}
            onChange={(fireRatePct) => {
              onChange({ ...draft, fireRatePct });
            }}
          />
        </Stack>
      </Section>
    </Stack>
  );
}
