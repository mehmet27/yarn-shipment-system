import { useTranslation } from '@iplik/i18n';
import {
  createBrandPalette,
  DEFAULT_BRAND_COLOR,
  isValidHexColor,
  SectionCard,
  Stack,
} from '@iplik/ui';
import type { BrandingDraft } from '../brandingDraft';
import { accentOf, PREVIEW_SCHEMES } from '@iplik/ui';
import { PreviewShell } from './PreviewShell';

export type BrandingPreviewProps = { draft: BrandingDraft };

/** Kayıttan önce sonucu gösterir: aynı ad, logo ve renk açık ve koyu görünümde alt alta. Uygulamanın temasını değiştirmez. */
export function BrandingPreview({ draft }: BrandingPreviewProps) {
  const { t } = useTranslation();
  const palette = createBrandPalette(
    isValidHexColor(draft.primaryColor) ? draft.primaryColor : DEFAULT_BRAND_COLOR,
  );
  const name = draft.companyName.trim() === '' ? t('app.name') : draft.companyName.trim();
  return (
    <SectionCard title={t('branding.preview.title')}>
      <Stack gap="md">
        {PREVIEW_SCHEMES.map((scheme) => (
          <PreviewShell
            key={scheme.key}
            name={name}
            logo={draft.logoDataUrl}
            accent={accentOf(palette, scheme)}
            scheme={scheme}
          />
        ))}
      </Stack>
    </SectionCard>
  );
}
