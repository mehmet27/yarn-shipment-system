import { useTranslation } from '@iplik/i18n';
import { Box, Group, Stack, Text, UnstyledButton } from '@iplik/ui';
import { PRESET_COLORS } from '@iplik/ui';

const TARGET_PX = 44;
const SWATCH_PX = 28;
const RING_PX = 2;
const RING = `${String(RING_PX)}px solid var(--app-text)`;

export type ColorPresetsProps = { value: string; onPick: (color: string) => void };

/** Hazır renkler: tek dokunuşla seçilir; seçili olanın çevresinde halka olur. */
export function ColorPresets({ value, onPick }: ColorPresetsProps) {
  const { t } = useTranslation();
  const label = t('branding.presets');
  return (
    <Stack gap={4}>
      <Text size="sm" fw={500}>
        {label}
      </Text>
      <Group gap={0} role="group" aria-label={label}>
        {PRESET_COLORS.map((preset) => {
          const isSelected = preset.value === value.toLowerCase();
          return (
            <UnstyledButton
              key={preset.key}
              w={TARGET_PX}
              h={TARGET_PX}
              aria-label={t(`branding.presetNames.${preset.key}`)}
              aria-pressed={isSelected}
              title={t(`branding.presetNames.${preset.key}`)}
              onClick={() => {
                onPick(preset.value);
              }}
              style={{ display: 'grid', placeItems: 'center' }}
            >
              <Box
                w={SWATCH_PX}
                h={SWATCH_PX}
                style={{
                  background: preset.value,
                  borderRadius: '50%',
                  outline: isSelected ? RING : undefined,
                  outlineOffset: RING_PX,
                }}
              />
            </UnstyledButton>
          );
        })}
      </Group>
    </Stack>
  );
}
