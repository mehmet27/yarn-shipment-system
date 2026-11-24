import { Anchor } from '@iplik/ui';
import { Link } from 'react-router';

/** Fasoncu adı, o fasoncunun sayfasına bağlantı olarak. */
export function SubcontractorLink({ id, title }: { id: string; title: string }) {
  return (
    <Anchor component={Link} to={`/panel/bakiye/fasoncu/${id}`}>
      {title}
    </Anchor>
  );
}
