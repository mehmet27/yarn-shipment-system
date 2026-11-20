import { Box, Text } from '@mantine/core';
import { NavIcon } from './NavIcon';

export type ProgressStepProps = {
  label: string;
  /** Adımın sırası (0'dan başlar). */
  index: number;
  /** Süreçteki geçerli adımın sırası. */
  current: number;
  /** Adın altındaki soluk açıklama, örn. adımı kimin yaptığı. */
  caption?: string | undefined;
};

const CIRCLE_SIZE_PX = 28;
const CHECK_SIZE_PX = 16;
const LINE_TOP_PX = 13;
const LINE_HEIGHT_PX = 2;
const TEXT_COLOR = 'var(--app-text)';
const LINE_COLOR = 'var(--app-line)';
const BOLD = 600;
const REACHED_CIRCLE = { bg: TEXT_COLOR, c: 'var(--app-surface)' } as const;
const PENDING_CIRCLE = { bg: LINE_COLOR, c: 'dimmed' } as const;

/** Adım çubuğunun tek adımı: numara dairesi (biten adımda onay işareti), önceki adıma bağlanan çizgi ve ad. */
export function ProgressStep({ label, index, current, caption }: ProgressStepProps) {
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
        {...(isReached ? REACHED_CIRCLE : PENDING_CIRCLE)}
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
        {index < current ? <NavIcon name="check" size={CHECK_SIZE_PX} /> : index + 1}
      </Box>
      <Text size="xs" fw={isCurrent ? BOLD : undefined} c={isCurrent ? TEXT_COLOR : 'dimmed'}>
        {label}
      </Text>
      {caption === undefined ? null : (
        <Text size="xs" c="dimmed">
          {caption}
        </Text>
      )}
    </Box>
  );
}
