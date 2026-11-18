import type { NavIconName } from '@iplik/ui';
import { Badge, SectionCard } from '@iplik/ui';
import type { ReactNode } from 'react';

/** Form bölümü kartı; `step` verilirse başlığın sağında sıra numarası görünür (taraflar → kaynak → iplikler). */
export function Section({
  title,
  icon,
  step,
  children,
}: {
  title: string;
  icon?: NavIconName;
  step?: number;
  children: ReactNode;
}) {
  return (
    <SectionCard
      title={title}
      {...(icon === undefined ? {} : { icon })}
      {...(step === undefined
        ? {}
        : {
            actions: (
              <Badge circle variant="default" aria-hidden>
                {step}
              </Badge>
            ),
          })}
    >
      {children}
    </SectionCard>
  );
}
