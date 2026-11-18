import { useTranslation } from '@iplik/i18n';
import { ColorInput, SectionCard, Stack } from '@iplik/ui';
import { ColorPresets } from './ColorPresets';

export type ColorSectionProps = {
  value: string;
  hasError: boolean;
  onChange: (color: string) => void;
};

/** Renk bölümü: üstte hazır renkler, altında elle giriş ve renk seçici. */
export function ColorSection({ value, hasError, onChange }: ColorSectionProps) {
  const { t } = useTranslation();
  return (
    <SectionCard title={t('branding.sections.color')}>
      <Stack>
        <ColorPresets value={value} onPick={onChange} />
        <ColorInput
          label={t('branding.primaryColor')}
          description={t('branding.primaryColorHint')}
          format="hex"
          eyeDropperButtonProps={{ 'aria-label': t('branding.eyeDropper') }}
          value={value}
          error={hasError ? t('branding.errors.color') : undefined}
          onChange={onChange}
        />
      </Stack>
    </SectionCard>
  );
}
