import { Switch } from '@iplik/ui';
import { useSiteChoices } from '../hooks/useSiteChoices';

/** Etkin lokasyonlar anahtar listesi olarak seçilir (çoklu seçim, mobilde büyük dokunma alanı). */
export function SiteSwitches({
  selected,
  onChange,
}: {
  selected: readonly string[];
  onChange: (next: readonly string[]) => void;
}) {
  const sites = useSiteChoices();
  return (
    <>
      {(sites.data ?? []).map((site) => (
        <Switch
          key={site.id}
          label={site.name}
          checked={selected.includes(site.id)}
          onChange={(event) => {
            onChange(
              event.currentTarget.checked
                ? [...selected, site.id]
                : selected.filter((id) => id !== site.id),
            );
          }}
        />
      ))}
    </>
  );
}
