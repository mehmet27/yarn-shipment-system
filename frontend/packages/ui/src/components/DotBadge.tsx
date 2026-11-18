import { Badge, Box } from '@mantine/core';
import type { ReactNode } from 'react';

const DOT_SIZE_PX = 8;

export type DotBadgeProps = { color: string; children: ReactNode };

/** Noktalı durum rozeti: nötr kenarlı, solunda durum renginde nokta. */
export function DotBadge({ color, children }: DotBadgeProps) {
  return (
    <Badge
      variant="outline"
      color="gray"
      radius="xl"
      c="var(--app-text)"
      style={{ borderColor: 'var(--app-line)', whiteSpace: 'nowrap', flexShrink: 0 }}
      leftSection={
        <Box
          w={DOT_SIZE_PX}
          h={DOT_SIZE_PX}
          bg={`var(--mantine-color-${color}-filled)`}
          style={{ borderRadius: '50%' }}
        />
      }
    >
      {children}
    </Badge>
  );
}
