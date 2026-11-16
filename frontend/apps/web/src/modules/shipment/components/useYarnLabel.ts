import { useTranslation } from '@iplik/i18n';

/** Sunucu, iplik cinsi belirtilmemiş hareketleri bu değerle saklar; ekranda düz Türkçe gösterilir. */
const UNSPECIFIED_YARN = '(reçetesiz)';

export function useYarnLabel() {
  const { t } = useTranslation();
  return (yarnType: string) =>
    yarnType === UNSPECIFIED_YARN ? t('settlement.balances.unspecifiedYarn') : yarnType;
}
