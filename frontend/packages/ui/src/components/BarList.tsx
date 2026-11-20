import { Box, Group, Stack, Text } from '@mantine/core';

export type BarListTone = 'neutral' | 'danger';

export type BarListItem = {
  key: string;
  label: string;
  /** Çubuğun uzunluğunu belirler; sıfırın altı çubuk çizmez. */
  value: number;
  /** Satırın sağında yazan, biçimlenmiş değer (ör. `1.982,5 kg`). */
  valueLabel: string;
  /** Adın altında tek satır açıklama (ör. "yolda 500 kg"). */
  note?: string | undefined;
  /** `danger`: dikkat isteyen satır; renge ek olarak `note` ile de söylenmelidir. */
  tone?: BarListTone | undefined;
};

export type BarListProps = {
  /** Ekran okuyucu için grafiğin adı. */
  label: string;
  items: readonly BarListItem[];
  testId?: string | undefined;
};

const BAR_HEIGHT = 8;
const BAR_RADIUS = 4;
const FULL = 100;
const TONE_COLOR: Readonly<Record<BarListTone, string>> = { neutral: 'blue', danger: 'red' };

function widthOf(value: number, max: number): string {
  return max <= 0 || value <= 0 ? '0%' : `${String((value / max) * FULL)}%`;
}

/**
 * Sıralı yatay çubuk listesi: her satırda ad, değer ve altında çubuk. Dar ekranda (360 px) ad ile çubuk yan yana
 * sıkışmaz; değer her satırda yazıyla da durur, yani bilgi yalnızca çubuğun boyuna ya da rengine bağlı değildir.
 */
export function BarList({ label, items, testId }: BarListProps) {
  const max = Math.max(0, ...items.map((item) => item.value));
  return (
    <Stack gap="sm" role="list" aria-label={label} data-testid={testId}>
      {items.map((item) => (
        <Stack key={item.key} gap={4} role="listitem" data-testid="bar-list-row">
          <Group justify="space-between" align="baseline" wrap="nowrap" gap="sm">
            <Text size="sm" fw={500} style={{ minWidth: 0, overflowWrap: 'anywhere' }}>
              {item.label}
            </Text>
            <Text
              size="sm"
              fw={600}
              style={{ whiteSpace: 'nowrap', fontVariantNumeric: 'tabular-nums' }}
            >
              {item.valueLabel}
            </Text>
          </Group>
          <Box h={BAR_HEIGHT} bg="var(--app-line)" style={{ borderRadius: BAR_RADIUS }} aria-hidden>
            <Box
              h={BAR_HEIGHT}
              w={widthOf(item.value, max)}
              bg={`var(--mantine-color-${TONE_COLOR[item.tone ?? 'neutral']}-filled)`}
              style={{ borderRadius: BAR_RADIUS }}
              data-testid="bar-list-fill"
            />
          </Box>
          {item.note === undefined ? null : (
            <Text
              size="xs"
              c={item.tone === 'danger' ? 'var(--mantine-color-red-light-color)' : 'dimmed'}
            >
              {item.note}
            </Text>
          )}
        </Stack>
      ))}
    </Stack>
  );
}
