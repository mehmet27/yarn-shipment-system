import { Box, Group, Stack, Text } from '@mantine/core';
import { NavIcon } from './NavIcon';

export type StepListItem = {
  label: string;
  /** Adımın altındaki soluk açıklama, örn. adımı kimin yaptığı. */
  caption?: string | undefined;
};

export type StepListProps = {
  steps: readonly StepListItem[];
  /** Geçerli adımın sırası (0'dan başlar); adım sayısına eşitse hepsi bitmiştir. */
  current: number;
  /** Liste için erişilebilir ad. */
  label: string;
};

const CIRCLE_SIZE_PX = 24;
const CHECK_SIZE_PX = 14;
const TEXT_COLOR = 'var(--app-text)';
const LINE_COLOR = 'var(--app-line)';
const BOLD = 600;

/** Dikey adım listesi (dar ekran): biten adımda onay işareti, geçerli adım kalın, bekleyenler soluk. */
export function StepList({ steps, current, label }: StepListProps) {
  return (
    <Stack component="ol" gap="xs" aria-label={label} p={0} m={0}>
      {steps.map((step, index) => {
        const isReached = index <= current;
        const isCurrent = index === current;
        return (
          <Group
            key={step.label}
            component="li"
            gap="sm"
            wrap="nowrap"
            align="flex-start"
            aria-current={isCurrent ? 'step' : undefined}
            style={{ listStyle: 'none' }}
          >
            <Box
              w={CIRCLE_SIZE_PX}
              h={CIRCLE_SIZE_PX}
              bg={isReached ? TEXT_COLOR : LINE_COLOR}
              c={isReached ? 'var(--app-surface)' : 'dimmed'}
              fw={BOLD}
              style={{
                flexShrink: 0,
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 'var(--mantine-font-size-xs)',
              }}
            >
              {index < current ? <NavIcon name="check" size={CHECK_SIZE_PX} /> : index + 1}
            </Box>
            <Stack gap={0}>
              <Text
                size="sm"
                fw={isCurrent ? BOLD : undefined}
                c={isReached ? TEXT_COLOR : 'dimmed'}
              >
                {step.label}
              </Text>
              {step.caption === undefined ? null : (
                <Text size="xs" c="dimmed">
                  {step.caption}
                </Text>
              )}
            </Stack>
          </Group>
        );
      })}
    </Stack>
  );
}
