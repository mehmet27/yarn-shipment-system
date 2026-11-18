import { useTranslation } from '@iplik/i18n';
import { Box, Group, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { useBranding } from './useBranding';

const LOGO_HEIGHT_PX = 32;
const TOUCH_TARGET_PX = 44;
const MARK_SIZE_PX = 28;

export type BrandMarkProps = {
  /** Varsayılan: tıklayınca ana sayfaya gider. Giriş ve şifre sayfalarında bağlantı olmaz; oradan ana sayfaya gitmek sayfayı yeniler. */
  linked?: boolean;
};

/** Üst bardaki marka: varsa logo, yanında firma adı; hiçbiri kaydedilmemişse ürün adı. */
export function BrandMark({ linked = true }: BrandMarkProps) {
  const { t } = useTranslation();
  const branding = useBranding().data;
  const name = branding?.companyName ?? t('app.name');
  const logo = branding?.logoDataUrl;
  const content = (
    <Group gap="xs" wrap="nowrap">
      {logo === undefined ? (
        <Box
          aria-hidden
          w={MARK_SIZE_PX}
          h={MARK_SIZE_PX}
          bg="var(--mantine-color-brand-filled)"
          style={{ borderRadius: 8, flexShrink: 0 }}
        />
      ) : (
        <Box
          component="img"
          src={logo}
          alt=""
          h={LOGO_HEIGHT_PX}
          style={{ objectFit: 'contain' }}
        />
      )}
      <Text fw={600} truncate>
        {name}
      </Text>
    </Group>
  );
  const style = { textDecoration: 'none', display: 'flex', alignItems: 'center' } as const;
  return linked ? (
    <Box component={Link} to="/" c="inherit" mih={TOUCH_TARGET_PX} style={style}>
      {content}
    </Box>
  ) : (
    <Box c="inherit" mih={TOUCH_TARGET_PX} style={style}>
      {content}
    </Box>
  );
}
