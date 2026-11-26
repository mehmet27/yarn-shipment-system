import { useTranslation } from '@iplik/i18n';
import { Button, Divider, Group, Stack } from '@iplik/ui';
import { Fragment } from 'react';
import type { RecipeLineDraft } from './createDraft';
import { newLine } from './createDraft';
import { RecipeLineRow } from './RecipeLineRow';

export type RecipeLinesProps = {
  lines: readonly RecipeLineDraft[];
  onChange: (lines: readonly RecipeLineDraft[]) => void;
  /** Gönderim denendikten sonra satır hataları görünür. */
  showErrors: boolean;
  /** İlk bu kadar satır silinemez (kayıtlı satırlar yalnız düzeltilir); varsayılan 0. */
  lockedCount?: number;
  /** Kapıdan aktarmada seçili tedarikçi; parti listesi ona göre süzülür. */
  supplierId?: string;
};

/** Reçete satırları: iplik cinsi (liste ya da serbest metin), isteğe bağlı lot, net kg; en az bir satır. */
export function RecipeLines({
  lines,
  onChange,
  showErrors,
  lockedCount = 0,
  supplierId,
}: RecipeLinesProps) {
  const { t } = useTranslation();
  const update = (key: string, patch: Partial<RecipeLineDraft>) => {
    onChange(lines.map((line) => (line.key === key ? { ...line, ...patch } : line)));
  };
  return (
    <Stack gap="md" data-testid="recipe-lines">
      {lines.map((line, index) => (
        <Fragment key={line.key}>
          {index > 0 && <Divider color="var(--app-line)" />}
          <RecipeLineRow
            {...(supplierId === undefined ? {} : { supplierId })}
            line={line}
            index={index}
            removable={lines.length > 1 && index >= lockedCount}
            showErrors={showErrors}
            onPatch={(patch) => {
              update(line.key, patch);
            }}
            onRemove={() => {
              onChange(lines.filter((candidate) => candidate.key !== line.key));
            }}
          />
        </Fragment>
      ))}
      <Group justify="space-between">
        <Button
          variant="default"
          onClick={() => {
            onChange([...lines, newLine()]);
          }}
        >
          {t('create.recipe.add')}
        </Button>
      </Group>
    </Stack>
  );
}
