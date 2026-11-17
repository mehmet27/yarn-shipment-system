import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';

const KNOWN_ERRORS = ['RECIPE_WEIGHT_MISMATCH', 'INVALID_SHIPMENT_STATUS'] as const;

/** Kayıt hatası; kilolar tutmadıysa hiçbir şeyin kaydedilmediği söylenir. */
export function RecipeError({ code }: { code: string }) {
  const { t } = useTranslation();
  const known = KNOWN_ERRORS.find((candidate) => candidate === code);
  return (
    <Alert color="red" data-testid="recipe-error">
      {t(`recipeEdit.errors.${known ?? 'unknown'}`)}
    </Alert>
  );
}
