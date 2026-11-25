import { useTranslation } from '@iplik/i18n';
import { Button, Group, Select, Stack, TextInput } from '@iplik/ui';
import type { RecipeDraft } from '../recipeDraft';

const NUMBER_STYLES = { input: { textAlign: 'right' } } as const;

export type RecipeLinesProps = {
  lines: RecipeDraft['lines'];
  /** Seçilebilecek iplik adları (ürün listesi). */
  names: readonly string[];
  onChange: (lines: RecipeDraft['lines']) => void;
};

/** Karışımın iplik satırları: iplik listeden seçilir, oran yazılır; satır eklenir ve silinir. */
export function RecipeLines({ lines, names, onChange }: RecipeLinesProps) {
  const { t } = useTranslation();
  const setLine = (index: number, patch: Partial<RecipeDraft['lines'][number]>) => {
    onChange(lines.map((line, i) => (i === index ? { ...line, ...patch } : line)));
  };
  return (
    <Stack gap="xs">
      {lines.map((line, index) => (
        // eslint-disable-next-line react/no-array-index-key -- kimliksiz taslak satırı
        <Group key={index} align="flex-end" wrap="nowrap">
          <Select
            style={{ flex: 1 }}
            label={t('masterData.recipes.yarn')}
            placeholder={t('masterData.recipes.yarnPlaceholder')}
            data={[...names]}
            searchable
            value={line.yarnType === '' ? null : line.yarnType}
            onChange={(value) => {
              setLine(index, { yarnType: value ?? '' });
            }}
            allowDeselect={false}
          />
          <TextInput
            w={110}
            label={t('masterData.recipes.ratio')}
            inputMode="decimal"
            styles={NUMBER_STYLES}
            value={line.ratio}
            onChange={(event) => {
              setLine(index, { ratio: event.currentTarget.value });
            }}
          />
          <Button
            variant="default"
            onClick={() => {
              onChange(lines.filter((_, i) => i !== index));
            }}
          >
            {t('masterData.recipes.removeYarn')}
          </Button>
        </Group>
      ))}
      <Button
        variant="default"
        w="fit-content"
        onClick={() => {
          onChange([...lines, { yarnType: '', ratio: '' }]);
        }}
      >
        {t('masterData.recipes.addYarn')}
      </Button>
    </Stack>
  );
}
