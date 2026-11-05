import { useTranslation } from '@iplik/i18n';
import { Box, Group, Text } from '@iplik/ui';
import { useBranding } from './useBranding';

const LOGO_HEIGHT_PX = 32;

/** Üst bardaki marka: varsa logo, yanında firma adı; hiçbiri kaydedilmemişse ürün adı. */
export function BrandMark() {
  const { t } = useTranslation();
  const branding = useBranding().data;
  const name = branding?.companyName ?? t('app.name');
  const logo = branding?.logoDataUrl;
  return (
    <Group gap="xs" wrap="nowrap">
      {logo === undefined ? null : (
        <Box
          component="img"
          src={logo}
          alt=""
          h={LOGO_HEIGHT_PX}
          style={{ objectFit: 'contain' }}
        />
      )}
      <Text fw={700} size="lg" truncate>
        {name}
      </Text>
    </Group>
  );
}
