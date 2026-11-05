import { useTranslation } from '@iplik/i18n';
import { Box, Group, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { useBranding } from './useBranding';

const LOGO_HEIGHT_PX = 32;
const TOUCH_TARGET_PX = 44;

/** Üst bardaki marka: varsa logo, yanında firma adı; hiçbiri kaydedilmemişse ürün adı. Tıklayınca ana sayfaya gider. */
export function BrandMark() {
  const { t } = useTranslation();
  const branding = useBranding().data;
  const name = branding?.companyName ?? t('app.name');
  const logo = branding?.logoDataUrl;
  return (
    <Box
      component={Link}
      to="/"
      c="inherit"
      mih={TOUCH_TARGET_PX}
      style={{ textDecoration: 'none', display: 'flex', alignItems: 'center' }}
    >
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
    </Box>
  );
}
