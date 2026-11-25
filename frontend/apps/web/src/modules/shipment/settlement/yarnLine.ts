import type { FabricYarn } from '@iplik/api-client/fabric';
import type { useTranslation } from '@iplik/i18n';

type Translate = ReturnType<typeof useTranslation>['t'];

/** "İplik · Parti X · %60,5": kumaşın bir ipliği tek satırda. */
export function yarnLineText(
  t: Translate,
  yarn: Pick<FabricYarn, 'yarnType' | 'lotNo' | 'ratioPct'>,
) {
  const ratio = yarn.ratioPct.replace('.', ',');
  return yarn.lotNo === undefined
    ? t('settlement.fabric.yarnLine', { yarn: yarn.yarnType, ratio })
    : t('settlement.fabric.yarnLineLot', { yarn: yarn.yarnType, lot: yarn.lotNo, ratio });
}
