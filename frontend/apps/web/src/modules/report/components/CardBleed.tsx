import { Box } from '@iplik/ui';
import type { ReactNode } from 'react';

/** Bölüm kartının iç boşluğunu geri alır: hücre ızgarası ve tablo kartı kenardan kenara kaplar. */
export function CardBleed({ children }: { children: ReactNode }) {
  return <Box style={{ margin: 'calc(var(--mantine-spacing-md) * -1)' }}>{children}</Box>;
}
