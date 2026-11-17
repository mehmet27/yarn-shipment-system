import type { RecipeLineView, WeightDecisionKind } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Paper, Stack, TextInput, Title } from '@iplik/ui';
import { useState } from 'react';
import { useWeightDecision } from '../hooks/useWeightDecision';
import { DecisionPicker } from './DecisionPicker';
import { RecipeCorrectionFields } from './RecipeCorrectionFields';
import { correctionDifference, initialCorrection, toCorrectionLines } from './recipeCorrection';

const MAX_REASON_LENGTH = 500;

const KNOWN_CODES = ['INVALID_SHIPMENT_STATUS', 'INVALID_WEIGHT_DECISION'];

function errorKey(code: string): string {
  return `shipment.weightDecision.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

export type WeightDecisionFormProps = {
  shipmentId: string;
  /** Sevkiyatın iplik listesi; "iplik listesini güncelle" kararında müdür bu satırları düzeltir. */
  lines: readonly RecipeLineView[];
  /** Kantarda tartılan net kilo (API biçimi). */
  measuredKg: string;
};

/** Müdür ve yönetici: kantar uyuşmazlığında iptal, çuval ekle-çıkar ya da reçeteyi güncelle; gerekçe zorunlu. */
export function WeightDecisionForm({ shipmentId, lines, measuredKg }: WeightDecisionFormProps) {
  const { t } = useTranslation();
  const [decision, setDecision] = useState<WeightDecisionKind>('ADD_REMOVE_BAGS');
  const [reason, setReason] = useState('');
  const [draft, setDraft] = useState(() => initialCorrection(lines));
  const mutation = useWeightDecision(shipmentId);
  const corrected = toCorrectionLines(lines, draft);
  const difference = correctionDifference(corrected, measuredKg);
  const isRecipeReady = decision !== 'UPDATE_RECIPE' || difference === '0.000';
  return (
    <Paper withBorder p="md" radius="md" data-testid="weight-decision">
      <Stack>
        <Title order={2} size="h4">
          {t('shipment.weightDecision.title')}
        </Title>
        <DecisionPicker decision={decision} onChange={setDecision} />
        {decision === 'UPDATE_RECIPE' && (
          <RecipeCorrectionFields
            lines={lines}
            draft={draft}
            measuredKg={measuredKg}
            difference={difference}
            onChange={(lineNo, value) => {
              setDraft({ ...draft, [lineNo]: value });
            }}
          />
        )}
        <TextInput
          label={t('shipment.weightDecision.reason')}
          withAsterisk
          maxLength={MAX_REASON_LENGTH}
          value={reason}
          onChange={(event) => {
            setReason(event.currentTarget.value);
          }}
        />
        {mutation.isError && (
          <Alert color="red">{t(errorKey(toProblem(mutation.error).code))}</Alert>
        )}
        <Button
          w="fit-content"
          disabled={reason.trim() === '' || !isRecipeReady}
          loading={mutation.isPending}
          onClick={() => {
            mutation.mutate({
              decision,
              reason: reason.trim(),
              ...(decision === 'UPDATE_RECIPE' && corrected !== undefined
                ? { lines: corrected }
                : {}),
            });
          }}
        >
          {t('shipment.weightDecision.submit')}
        </Button>
      </Stack>
    </Paper>
  );
}
