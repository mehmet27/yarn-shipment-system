import { Box, Group, Paper, Stack, Text } from '@mantine/core';
import type { ReactNode } from 'react';
import type { NavIconName } from './NavIcon';
import { NavIcon } from './NavIcon';

export type SectionTone = 'brand' | 'info' | 'success' | 'warning' | 'danger';

const TONE_COLOR: Readonly<Record<SectionTone, string>> = {
  brand: 'brand',
  info: 'blue',
  success: 'teal',
  warning: 'orange',
  danger: 'red',
};

export type SectionCardProps = {
  title: string;
  /** Başlığın solundaki ikon (dekoratif). */
  icon?: NavIconName;
  /** Başlığın altında tek satırlık yönlendirme. */
  description?: string;
  /** Başlık çubuğunun sağındaki eylemler. */
  actions?: ReactNode;
  tone?: SectionTone;
  testId?: string;
  children: ReactNode;
};

/**
 * Bölüm kartı: girintili (gri) başlık şeridi ve beyaz gövde. Olağan bölümler nötrdür; renk yalnızca dikkat isteyen
 * bölümde (uyarı, hata, bilgi, başarı) başlık şeridine ve sol kenar çizgisine gelir.
 */
export function SectionCard({
  title,
  icon,
  description,
  actions,
  tone = 'brand',
  testId,
  children,
}: SectionCardProps) {
  const color = TONE_COLOR[tone];
  const isSignal = tone !== 'brand';
  return (
    <Paper
      withBorder
      radius="md"
      component="section"
      aria-label={title}
      data-testid={testId}
      style={{ overflow: 'hidden' }}
    >
      <Group
        justify="space-between"
        wrap="nowrap"
        gap="sm"
        px="md"
        py="xs"
        mih={44}
        bg={isSignal ? `${color}.0` : 'var(--app-inset)'}
        style={{
          borderBottom: `1px solid ${isSignal ? `var(--mantine-color-${color}-2)` : 'var(--app-line)'}`,
          boxShadow: isSignal ? `inset 3px 0 0 var(--mantine-color-${color}-6)` : undefined,
        }}
      >
        <Group gap="xs" wrap="nowrap" miw={0}>
          {icon === undefined ? null : (
            <Box c={isSignal ? `${color}.8` : 'gray.7'} lh={0}>
              <NavIcon name={icon} />
            </Box>
          )}
          <Stack gap={0} miw={0}>
            <Text fw={600} c={isSignal ? `${color}.9` : 'gray.9'} size="sm">
              {title}
            </Text>
            {description === undefined ? null : (
              <Text size="xs" c="dimmed">
                {description}
              </Text>
            )}
          </Stack>
        </Group>
        {actions}
      </Group>
      <Box p="md">{children}</Box>
    </Paper>
  );
}
