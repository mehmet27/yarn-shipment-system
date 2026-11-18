import { Box, Text } from '@mantine/core';

export type ProgressStepProps = {
  label: string;
  /** Adımın sırası (0'dan başlar). */
  index: number;
  /** Süreçteki geçerli adımın sırası. */
  current: number;
};

const CIRCLE_SIZE_PX = 28;
const LINE_TOP_PX = 13;
const LINE_HEIGHT_PX = 2;
const TEXT_COLOR = 'var(--app-text)';
const LINE_COLOR = 'var(--app-line)';
const BOLD = 600;

/** Adım çubuğunun tek adımı: numara dairesi, önceki adıma bağlanan çizgi ve ad. */
export function ProgressStep({ label, index, current }: ProgressStepProps) {
  const isReached = index <= current;
  const isCurrent = index === current;
  return (
    <Box
      component="li"
      aria-current={isCurrent ? 'step' : undefined}
      style={{ flex: 1, textAlign: 'center', listStyle: 'none', position: 'relative' }}
    >
      {index === 0 ? null : (
        <Box
          aria-hidden
          pos="absolute"
          top={LINE_TOP_PX}
          h={LINE_HEIGHT_PX}
          bg={isReached ? TEXT_COLOR : LINE_COLOR}
          style={{ left: '-50%', width: '100%' }}
        />
      )}
      <Box
        pos="relative"
        w={CIRCLE_SIZE_PX}
        h={CIRCLE_SIZE_PX}
        mx="auto"
        mb={6}
        bg={isReached ? TEXT_COLOR : LINE_COLOR}
        c={isReached ? 'var(--app-surface)' : 'dimmed'}
        fw={BOLD}
        style={{
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1,
          fontSize: 'var(--mantine-font-size-sm)',
          boxShadow: isCurrent ? `0 0 0 4px ${LINE_COLOR}` : undefined,
        }}
      >
        {index + 1}
      </Box>
      <Text size="xs" fw={isCurrent ? BOLD : undefined} c={isCurrent ? TEXT_COLOR : 'dimmed'}>
        {label}
      </Text>
    </Box>
  );
}
