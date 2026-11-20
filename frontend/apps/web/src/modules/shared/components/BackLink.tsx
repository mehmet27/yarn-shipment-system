import { Anchor, Icon } from '@iplik/ui';
import type { ReactNode } from 'react';
import { Link } from 'react-router';

export type BackLinkProps = {
  to: string;
  children: ReactNode;
};

const ICON_SIZE_PX = 16;

/**
 * Sayfanın üstündeki "… dön" bağlantısı: solunda ok vardır ve yalnızca yazısı kadar yer kaplar; bağlantı olduğu
 * ilk bakışta anlaşılır, satırın boş kısmına dokununca yanlışlıkla geri gidilmez. Ok dekoratiftir; bağlantının adı
 * yalnızca yazısıdır.
 */
export function BackLink({ to, children }: BackLinkProps) {
  return (
    <Anchor component={Link} to={to} w="fit-content" style={{ gap: 6 }}>
      <Icon name="arrowLeft" size={ICON_SIZE_PX} />
      {children}
    </Anchor>
  );
}
