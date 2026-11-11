import type { CreateDraft, CreateFieldErrors } from './createDraft';
import { FireRateField } from './FireRateField';
import { RecipeLines } from './RecipeLines';

export type RecipeSectionProps = {
  draft: CreateDraft;
  onChange: (draft: CreateDraft) => void;
  errors: CreateFieldErrors;
  defaultFirePct: string;
};

/** Reçete satırları ve fire oranı birlikte; ikisi de sevkiyatla saklanır. */
export function RecipeSection({ draft, onChange, errors, defaultFirePct }: RecipeSectionProps) {
  return (
    <>
      <RecipeLines
        lines={draft.lines}
        showErrors={errors.lines !== undefined}
        onChange={(lines) => {
          onChange({ ...draft, lines });
        }}
      />
      <FireRateField
        value={draft.fireRatePct ?? defaultFirePct}
        errorCode={errors.fireRate}
        onChange={(fireRatePct) => {
          onChange({ ...draft, fireRatePct });
        }}
      />
    </>
  );
}
