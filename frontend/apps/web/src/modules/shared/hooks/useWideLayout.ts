import { useMediaQuery } from '@iplik/ui';

/**
 * Tablonun sığdığı en dar genişlik (992 px). Altında (telefon ve dik tutulan tablet) yan menü de açıkken içerik alanı
 * bir telefon kadar dardır; tablo yerine kart listesi gösterilir.
 */
const WIDE_LAYOUT_QUERY = '(min-width: 62em)';

/** Geniş tablo sığıyor mu? Listeler bu soruya göre tablo ya da kart çizer (FE-UI-07); eşik tek yerde durur. */
export function useWideLayout(): boolean {
  return useMediaQuery(WIDE_LAYOUT_QUERY, true, { getInitialValueInEffect: false });
}
