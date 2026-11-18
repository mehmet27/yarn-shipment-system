import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Stack, Text } from '@iplik/ui';
import { formatKgText, isZeroKg } from '@iplik/units';
import { useState } from 'react';
import type { YarnDemand } from './useCentralStockCheck';
import { useCentralStockCheck } from './useCentralStockCheck';

/** Eksik uyarısı koyuda doygun turuncuya dönmesin: nötr girinti, yalnızca sol kenarda sarı çizgi. */
const SHORT_STYLES = {
  root: {
    background: 'var(--app-inset)',
    border: '1px solid var(--app-line)',
    boxShadow: 'inset 3px 0 0 var(--mantine-color-yellow-filled)',
  },
  title: { color: 'var(--app-text)' },
  message: { color: 'var(--app-text)' },
} as const;

type Props = {
  demands: readonly YarnDemand[];
  /** `true`: yola çıkana kadar merkez stok her zaman görünür; `false`: yalnızca eksik varken, kapatılabilir uyarı. */
  alwaysShow: boolean;
};

/**
 * Merkez stoğu reçeteye yetiyor mu: eksik varsa sarı uyarı (kapatılabilir, sayfa her açılışında yeniden çıkar),
 * yetiyorsa sevkiyat yola çıkana kadar sakin bir bilgi kartı. Eksiği sevkiyat tamamlanınca sistem girer.
 */
export function CentralStockNotice({ demands, alwaysShow }: Props) {
  const { t } = useTranslation();
  const [isClosed, setIsClosed] = useState(false);
  const lines = useCentralStockCheck(demands);
  const short = lines.filter((line) => !isZeroKg(line.missingKg));
  if (lines.length === 0 || (!alwaysShow && short.length === 0)) return null;
  if (short.length === 0) {
    return (
      <Alert color="teal" title={t('stockNotice.okTitle')} data-testid="stock-notice">
        <Stack gap="xs">
          {lines.map((line) => (
            <Group key={line.yarn} justify="space-between" gap="xs">
              <Text size="sm">{line.yarn}</Text>
              <Text size="sm" ta="right">
                {t('stockNotice.line', {
                  stock: formatKgText(line.stockKg),
                  needed: formatKgText(line.neededKg),
                })}
              </Text>
            </Group>
          ))}
        </Stack>
      </Alert>
    );
  }
  if (isClosed) return null;
  return (
    <Alert
      styles={SHORT_STYLES}
      color="yellow"
      title={t('stockNotice.shortTitle')}
      data-testid="stock-notice"
    >
      <Stack gap="xs">
        {short.map((line) => (
          <Text key={line.yarn} size="sm">
            {t('stockNotice.shortItem', {
              yarn: line.yarn,
              stock: formatKgText(line.stockKg),
              missing: formatKgText(line.missingKg),
            })}
          </Text>
        ))}
        <Text size="sm" c="dimmed">
          {t('stockNotice.advice')}
        </Text>
        <Button
          variant="default"
          w="fit-content"
          onClick={() => {
            setIsClosed(true);
          }}
        >
          {t('stockNotice.close')}
        </Button>
      </Stack>
    </Alert>
  );
}
