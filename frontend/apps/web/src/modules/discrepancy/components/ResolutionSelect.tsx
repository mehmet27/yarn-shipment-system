import type { Resolution } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Select, Text } from '@iplik/ui';

export type ResolutionSelectProps = {
  choices: Resolution[];
  value: Resolution | undefined;
  onChange: (value: Resolution | undefined) => void;
};

/** Karar seçimi; altında seçilen kararın ne yaptığını tek cümleyle anlatır. */
export function ResolutionSelect({ choices, value, onChange }: ResolutionSelectProps) {
  const { t } = useTranslation();
  return (
    <>
      <Select
        label={t('discrepancy.resolve.resolution')}
        data={choices.map((item) => ({
          value: item,
          label: t(`discrepancy.resolve.choice.${item}`),
        }))}
        value={value ?? null}
        onChange={(selected) => {
          onChange(choices.find((item) => item === selected));
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
