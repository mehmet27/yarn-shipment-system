import { useTranslation } from '@iplik/i18n';
import { Select, Text } from '@iplik/ui';
import type { ResolveChoice } from './resolveChoices';

export type ResolutionSelectProps = {
  choices: ResolveChoice[];
  isManager: boolean;
  value: ResolveChoice | undefined;
  onChange: (value: ResolveChoice | undefined) => void;
};

/** Karar seçimi; altında seçilen kararın ne yaptığını tek cümleyle anlatır. */
export function ResolutionSelect({ choices, isManager, value, onChange }: ResolutionSelectProps) {
  const { t } = useTranslation();
  const labelKey = isManager ? 'discrepancy.resolve.choice' : 'discrepancy.resolution';
  return (
    <>
      <Select
        label={t('discrepancy.resolve.resolution')}
        data={choices.map((item) => ({ value: item, label: t(`${labelKey}.${item}`) }))}
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
