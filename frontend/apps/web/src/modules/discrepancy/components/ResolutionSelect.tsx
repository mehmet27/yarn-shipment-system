import { Resolution } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Select, Text } from '@iplik/ui';

const RESOLUTIONS = Object.values(Resolution);
/** Uyuşmazlıklar yalnızca kalite itirazıdır; "tolerans içinde" ve "veri hatası" kararları bu akışa uymaz. */
const OFFERED = RESOLUTIONS.filter((value) => value !== 'TOLERANCE' && value !== 'DATA_ERROR');

export type ResolutionSelectProps = {
  value: Resolution | undefined;
  onChange: (value: Resolution | undefined) => void;
};

/** Karar seçimi; altında seçilen kararın ne yaptığını tek cümleyle anlatır. */
export function ResolutionSelect({ value, onChange }: ResolutionSelectProps) {
  const { t } = useTranslation();
  return (
    <>
      <Select
        label={t('discrepancy.resolve.resolution')}
        data={OFFERED.map((item) => ({ value: item, label: t(`discrepancy.resolution.${item}`) }))}
        value={value ?? null}
        onChange={(selected) => {
          onChange(RESOLUTIONS.find((item) => item === selected));
        }}
        withAsterisk
      />
      {value !== undefined && (
        <Text size="sm" c="dimmed" data-testid="resolution-effect">
          {t(`discrepancy.resolve.effect.${value}`)}
        </Text>
      )}
    </>
  );
}
