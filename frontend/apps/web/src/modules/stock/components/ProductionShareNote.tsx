import { useTranslation } from '@iplik/i18n';
import { Text } from '@iplik/ui';
import { formatKgText } from '@iplik/units';
import { useCentralStock } from '../hooks/useCentralStock';
import type { ProductionDraft, YarnLineDraft } from '../stockDraft';
import { productionShare } from '../stockDraft';

/** "Kumaş 95 kg · stoktan 98,958 kg (fire dahil) · stokta 200 kg"; stok yetmiyorsa kırmızı. Kuralı sunucu uygular. */
export function ProductionShareNote({
  draft,
  row,
}: {
  draft: ProductionDraft;
  row: YarnLineDraft;
}) {
  const { t } = useTranslation();
  const stock = useCentralStock().data;
  const share = productionShare(draft, row);
  if (share === undefined) return null;
  const inStock =
    row.product === undefined || stock === undefined
      ? undefined
      : (stock.rows.find((r) => r.productId === row.product?.value)?.stockKg ?? '0.000');
  const isShort = inStock !== undefined && Number(inStock) < Number(share.yarnKg);
  return (
    <Text size="xs" c={isShort ? 'red' : 'dimmed'}>
      {t('stock.production.share', {
        fabric: formatKgText(share.fabricKg),
        yarn: formatKgText(share.yarnKg),
      })}
      {inStock !== undefined &&
        ` · ${t('stock.production.inStock', { kg: formatKgText(inStock) })}`}
    </Text>
  );
}
