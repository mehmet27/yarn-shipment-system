import { Box, Group, Paper, Text } from '@mantine/core';
import type { ReactNode } from 'react';

export type StatTone = 'neutral' | 'info' | 'warning' | 'danger' | 'success';

const TONE_COLOR: Record<StatTone, string> = {
  neutral: 'gray',
  info: 'blue',
  warning: 'yellow',
  danger: 'red',
  success: 'green',
};

export type StatCardProps = {
  label: string;
  value: ReactNode;
  /** Sayının anlamını renkle birlikte ikon ve metinle de verir (FE-UI-03). */
  tone?: StatTone;
  icon?: ReactNode;
  hint?: string;
};

/** Liste başındaki özet göstergesi: "6 işlem bekleyen" gibi. */
export function StatCard({ label, value, tone = 'neutral', icon, hint }: StatCardProps) {
  const color = TONE_COLOR[tone];
  return (
    <Paper p="md" withBorder>
      <Group justify="space-between" wrap="nowrap" align="flex-start">
        <Box>
          <Text size="sm" c="dimmed">
            {label}
          </Text>
          <Text fz={28} fw={700} lh={1.2}>
            {value}
          </Text>
          {hint === undefined ? null : (
            <Text size="xs" c={`${color}.8`} fw={600}>
              {hint}
            </Text>
          )}
        </Box>
        {icon === undefined ? null : (
          <Box
            aria-hidden
            p="xs"
            bg={`${color}.0`}
            c={`${color}.8`}
            style={{ borderRadius: 'var(--mantine-radius-md)', display: 'flex' }}
          >
            {icon}
          </Box>
        )}
      </Group>
    </Paper>
  );
}
