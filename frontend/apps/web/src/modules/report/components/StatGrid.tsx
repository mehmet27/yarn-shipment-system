import { Box } from '@iplik/ui';
import type { ReactNode } from 'react';

const MIN_CELL = '9rem';

/**
 * Kart içinde çizgiyle ayrılmış sayı hücreleri; kutu içinde kutu yoktur. Çizgileri hücrelerin gölgesi çizer: hücreler
 * arasında 1 px boşluk vardır, dış kenara taşan gölge kesilir (çerçeveyi kart çizer).
 */
export function StatGrid({ children }: { children: ReactNode }) {
  return (
    <Box
      style={{
        display: 'grid',
        gridTemplateColumns: `repeat(auto-fit, minmax(${MIN_CELL}, 1fr))`,
        gap: 1,
        overflow: 'hidden',
      }}
    >
      {children}
    </Box>
  );
}
