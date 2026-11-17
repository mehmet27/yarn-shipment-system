import type { NavIconName } from '@iplik/ui';
import { SectionCard } from '@iplik/ui';
import type { ReactNode } from 'react';

/** Renkli başlık çubuklu form kartı; formun bölümleri birbirinden net ayrılır. */
export function Section({
  title,
  icon,
  children,
}: {
  title: string;
  icon?: NavIconName;
  children: ReactNode;
}) {
  return (
    <SectionCard title={title} {...(icon === undefined ? {} : { icon })}>
      {children}
    </SectionCard>
  );
}
