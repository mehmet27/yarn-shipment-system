import { useTranslation } from '@iplik/i18n';
import { Stack, Text, Title } from '@iplik/ui';
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
      <Title order={1} size="h2">
        {t('discrepancy.settings.title')}
      </Title>
      <Text c="dimmed">{t('discrepancy.settings.intro')}</Text>
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
