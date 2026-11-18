import { useTranslation } from '@iplik/i18n';
import type { NavIconName } from '@iplik/ui';
import { Box, Group, Icon, SimpleGrid, Stack, Text, UnstyledButton } from '@iplik/ui';

export type DecisionOption = 'ADD_REMOVE_BAGS' | 'UPDATE_RECIPE' | 'CANCEL';

const OPTIONS: readonly { value: DecisionOption; icon: NavIconName; color: string }[] = [
  { value: 'ADD_REMOVE_BAGS', icon: 'edit', color: 'brand' },
  { value: 'UPDATE_RECIPE', icon: 'list', color: 'brand' },
  { value: 'CANCEL', icon: 'stop', color: 'red' },
];

type Props = { value: DecisionOption; onChange: (value: DecisionOption) => void };

/** Karar seçenekleri: renkli, ikonlu ve açıklamalı üç kart; seçili olan kalın çerçeveyle öne çıkar. */
export function WeightDecisionCards({ value, onChange }: Props) {
  const { t } = useTranslation();
  return (
    <SimpleGrid
      cols={{ base: 1, sm: 3 }}
      spacing="sm"
      role="radiogroup"
      aria-label={t('shipment.weightDecision.decision')}
    >
      {OPTIONS.map((option) => {
        const isSelected = option.value === value;
        return (
          <UnstyledButton
            key={option.value}
            role="radio"
            aria-checked={isSelected}
            onClick={() => {
              onChange(option.value);
            }}
            p="sm"
            style={{
              borderRadius: 'var(--mantine-radius-md)',
              border: `2px solid ${isSelected ? `var(--mantine-color-${option.color}-6)` : 'var(--app-line)'}`,
              background: isSelected
                ? `var(--mantine-color-${option.color}-0)`
                : 'var(--app-surface)',
            }}
          >
            <Group gap="xs" wrap="nowrap" align="flex-start">
              <Box c={`${option.color}.7`} lh={0} pt={2}>
                <Icon name={option.icon} />
              </Box>
              <Stack gap={2}>
                <Text fw={600} size="sm">
                  {t(`shipment.weightDecision.options.${option.value}`)}
                </Text>
                <Text size="xs" c="dimmed">
                  {t(`shipment.weightDecision.hints.${option.value}`)}
                </Text>
              </Stack>
            </Group>
          </UnstyledButton>
        );
      })}
    </SimpleGrid>
  );
}
