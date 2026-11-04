import { Box, Text } from '@mantine/core';

export type ProgressStepProps = {
  label: string;
  /** Adımın sırası (0'dan başlar). */
  index: number;
  /** Süreçteki geçerli adımın sırası. */
  current: number;
};

const CIRCLE_SIZE_PX = 32;
const LINE_TOP_PX = 15;
const LINE_HEIGHT_PX = 3;
const BOLD = 700;

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
          bg={isReached ? 'brand.7' : 'gray.3'}
          style={{ left: '-50%', width: '100%' }}
        />
      )}
      <Box
        pos="relative"
        w={CIRCLE_SIZE_PX}
        h={CIRCLE_SIZE_PX}
        mx="auto"
        mb={6}
        bg={isReached ? 'brand.7' : 'gray.2'}
        c={isReached ? 'white' : 'gray.7'}
        fw={BOLD}
        style={{
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1,
          boxShadow: isCurrent ? '0 0 0 5px var(--mantine-color-brand-1)' : undefined,
        }}
      >
        {index + 1}
      </Box>
      <Text size="xs" fw={isCurrent ? BOLD : undefined} c={isCurrent ? 'brand.8' : 'dimmed'}>
        {label}
      </Text>
    </Box>
  );
}
