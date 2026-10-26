import { useTranslation } from '@iplik/i18n';
import { Text } from '@iplik/ui';
import { SiteSwitches } from './SiteSwitches';

/** Lokasyon seçimi zorunludur; başlıkta yıldız, hatada altında ne yapılacağı yazar. */
export function SiteField({
  selected,
  onChange,
  error,
}: {
  selected: readonly string[];
  onChange: (next: readonly string[]) => void;
  error: string | undefined;
}) {
  const { t } = useTranslation();
  return (
    <>
      <Text fw={500}>
        {t('masterData.loginAccount.sites')}{' '}
        <Text span c="red" aria-hidden>
          *
        </Text>
      </Text>
      <SiteSwitches selected={selected} onChange={onChange} />
      {error !== undefined && (
        <Text c="red" size="sm">
          {error}
        </Text>
      )}
    </>
  );
}
