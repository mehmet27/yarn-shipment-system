import { useTranslation } from '@iplik/i18n';
import type { FieldValue } from './shipmentViews';

/** Değeri ya da yokluğunun nedenini ("Kaydedilmedi", "Görme yetkiniz yok", "Kör sayım") yazar. */
export function useFieldText() {
  const { t } = useTranslation();
  return (field: FieldValue) => ('value' in field ? field.value : t(field.missing));
}
