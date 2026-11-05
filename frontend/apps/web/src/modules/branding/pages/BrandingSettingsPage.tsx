import { useTranslation } from '@iplik/i18n';
import { Alert, Center, Loader, PageHero, Stack } from '@iplik/ui';
import { BrandingForm } from '../components/BrandingForm';
import { useBranding } from '../useBranding';

/** Yönetim > Görünüm: kurulumun marka rengi, firma adı ve logosu (yalnızca SETTINGS_MANAGE). */
export function BrandingSettingsPage() {
  const { t } = useTranslation();
  const query = useBranding();
  const branding = query.isPlaceholderData ? undefined : query.data;
  return (
    <Stack>
      <PageHero title={t('branding.title')} subtitle={t('branding.intro')} />
      {branding !== undefined && <BrandingForm branding={branding} />}
      {branding === undefined && query.isError && (
        <Alert color="red">{t('branding.loadFailed')}</Alert>
      )}
      {branding === undefined && !query.isError && (
        <Center py="xl">
          <Loader />
        </Center>
      )}
    </Stack>
  );
}
