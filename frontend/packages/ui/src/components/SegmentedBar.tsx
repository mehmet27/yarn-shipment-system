import { Box, Group, Stack, Text } from '@mantine/core';

export type SegmentedBarPart = {
  key: string;
  label: string;
  /** Dilimin payını belirler; sıfır dilim çizilmez ama açıklamada yazar. */
  value: number;
  /** Açıklamada yazan biçimlenmiş değer. */
  valueLabel: string;
  /** Mantine renk adı (`blue`, `green`, `gray`...). */
  color: string;
};

export type SegmentedBarProps = {
  /** Ekran okuyucu için grafiğin adı. */
  label: string;
  parts: readonly SegmentedBarPart[];
  testId?: string | undefined;
};

const BAR_HEIGHT = 12;
const BAR_RADIUS = 6;
const GAP = 2;
const SWATCH = 10;
const SWATCH_RADIUS = 3;
const LEGEND_ROW_GAP = 4;

/**
 * Bir bütünün parçalara dağılımı: ince dilimli çubuk ve altında renk kutusu + ad + değer açıklaması. Değerler
 * açıklamada yazıyla durur; çubuk yalnızca oranı gösterir.
 */
export function SegmentedBar({ label, parts, testId }: SegmentedBarProps) {
  const drawn = parts.filter((part) => part.value > 0);
  return (
    <Stack gap="xs" data-testid={testId}>
      <Group
        gap={GAP}
        wrap="nowrap"
        h={BAR_HEIGHT}
        role="img"
        aria-label={`${label}: ${parts.map((part) => `${part.label} ${part.valueLabel}`).join(', ')}`}
        bg={drawn.length === 0 ? 'var(--app-line)' : 'transparent'}
        style={{ borderRadius: BAR_RADIUS, overflow: 'hidden' }}
      >
        {drawn.map((part) => (
          <Box
            key={part.key}
            h={BAR_HEIGHT}
            bg={`var(--mantine-color-${part.color}-filled)`}
            style={{ flexGrow: part.value, flexBasis: 0, minWidth: GAP }}
            data-testid="segmented-bar-part"
          />
        ))}
      </Group>
      <Group gap="md" style={{ rowGap: LEGEND_ROW_GAP }}>
        {parts.map((part) => (
          <Group key={part.key} gap={6} wrap="nowrap">
            <Box
              w={SWATCH}
              h={SWATCH}
              bg={`var(--mantine-color-${part.color}-filled)`}
              style={{ borderRadius: SWATCH_RADIUS, flexShrink: 0 }}
              aria-hidden
            />
            <Text size="sm" c="dimmed">
              {part.label}
            </Text>
            <Text size="sm" fw={600}>
              {part.valueLabel}
            </Text>
          </Group>
        ))}
      </Group>
    </Stack>
  );
}
