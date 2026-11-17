import type { ShipmentDetail } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, Group, Icon, SectionCard, Stack } from '@iplik/ui';
import { useState } from 'react';
import type { RecipeLineDraft } from '../create/createDraft';
import { lineError, toRecipeInputs, totalNetKg } from '../create/createDraft';
import { RecipeLines } from '../create/RecipeLines';
import { findWeightCheck } from '../components/shipmentViews';
import { useUpdateRecipe } from '../hooks/useUpdateRecipe';
import { useWeightTolerance } from '../hooks/useWeightTolerance';
import { draftFromLine } from './recipeDrafts';
import { ReasonCard } from './RecipeEditorParts';
import { RecipeError } from './RecipeError';
import { RecipeWeightStatus } from './RecipeWeightStatus';

type Props = { shipment: ShipmentDetail; onDone: () => void };

/**
 * "İplik güncelle": müdür reçeteyi tartıya göre düzeltir. Toplam kilo tartıyı (tolerans içinde) tutmadan kaydedilemez;
 * tutunca sevkiyat yola çıkar. Kayıtlı satırlar silinemez, yalnız düzeltilir; yeni satır eklenebilir.
 */
export function RecipeEditor({ shipment, onDone }: Props) {
  const { t } = useTranslation();
  const saved = shipment.lines ?? [];
  const [lines, setLines] = useState<readonly RecipeLineDraft[]>(() => saved.map(draftFromLine));
  const [reason, setReason] = useState('');
  const [isShowingErrors, setIsShowingErrors] = useState(false);
  const tolerance = useWeightTolerance();
  const mutation = useUpdateRecipe(shipment.shipmentId);
  const measuredKg = findWeightCheck(shipment)?.measuredKg;
  const inputs = toRecipeInputs(lines);
  const total = totalNetKg(lines);
  const submit = () => {
    setIsShowingErrors(true);
    if (inputs === undefined || reason.trim() === '') return;
    mutation.mutate({ lines: inputs, reason: reason.trim() }, { onSuccess: onDone });
  };
  const problem = mutation.isError ? toProblem(mutation.error) : undefined;
  return (
    <Stack>
      {measuredKg !== undefined && (
        <RecipeWeightStatus recipeKg={total} measuredKg={measuredKg} toleranceKg={tolerance} />
      )}
      <SectionCard
        title={t('recipeEdit.linesTitle')}
        description={t('recipeEdit.linesHint')}
        icon="list"
        tone="info"
      >
        <RecipeLines
          lines={lines}
          lockedCount={saved.length}
          showErrors={isShowingErrors && lines.some((line) => lineError(line) !== undefined)}
          onChange={(next) => {
            setLines(next);
            if (mutation.isError) mutation.reset();
          }}
        />
      </SectionCard>
      <ReasonCard reason={reason} onChange={setReason} />
      {problem !== undefined && <RecipeError code={problem.code} />}
      <Group>
        <Button
          leftSection={<Icon name="truck" />}
          loading={mutation.isPending}
          disabled={inputs === undefined || reason.trim() === ''}
          onClick={submit}
        >
          {t('recipeEdit.submit')}
        </Button>
      </Group>
    </Stack>
  );
}
