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
 * Ayrışan bölüm kartı: renkli başlık çubuğu ve beyaz gövde. Sayfadaki bölümlerin birbirinden net ayrılması ve
 * kullanıcıyı "şimdi ne yapıyorum" sorusuna yönlendirmesi içindir.
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
  return (
    <Paper
      withBorder
      radius="md"
      shadow="xs"
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
        bg={`${color}.0`}
        style={{
          borderBottom: `1px solid var(--mantine-color-${color}-2)`,
          borderLeft: `4px solid var(--mantine-color-${color}-6)`,
        }}
      >
        <Group gap="xs" wrap="nowrap" miw={0}>
          {icon === undefined ? null : (
            <Box c={`${color}.7`} lh={0}>
              <NavIcon name={icon} />
            </Box>
          )}
          <Stack gap={0} miw={0}>
            <Text fw={700} c={`${color}.9`} size="sm">
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
