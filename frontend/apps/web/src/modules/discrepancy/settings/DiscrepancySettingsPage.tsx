import { useTranslation } from '@iplik/i18n';
import { PageHero, Stack } from '@iplik/ui';
import { QueryState } from '@/modules/shipment';
import { useDiscrepancySettings } from '../hooks/useDiscrepancySettings';
import { SettingsForm } from './SettingsForm';
import { SettingsHistory } from './SettingsHistory';

/** Admin: miktar tolerans bantları ve yolda kalma sınırı; her değişiklik yeni sürüm olur, eskisi geçmişte kalır. */
export function DiscrepancySettingsPage() {
  const { t } = useTranslation();
  const query = useDiscrepancySettings();
  return (
    <Stack>
      <PageHero
        title={t('discrepancy.settings.title')}
        subtitle={t('discrepancy.settings.intro')}
      />
      <QueryState query={query}>
        {(settings) => (
          <Stack>
            <SettingsForm settings={settings} />
            <SettingsHistory settings={settings} />
          </Stack>
        )}
      </QueryState>
    </Stack>
  );
}
