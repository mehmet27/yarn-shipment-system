import { Box } from '@iplik/ui';
import type { ReactNode } from 'react';

const MIN_CELL = '9rem';

/** Kart içinde çizgiyle ayrılmış sayı hücreleri; kutu içinde kutu yoktur. Çizgileri hücrelerin gölgesi çizer. */
export function StatGrid({ children }: { children: ReactNode }) {
  return (
    <Box
      style={{
        display: 'grid',
        gridTemplateColumns: `repeat(auto-fit, minmax(${MIN_CELL}, 1fr))`,
        overflow: 'hidden',
      }}
    >
      {children}
    </Box>
  );
}
