import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Icon, SectionCard, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { Link } from 'react-router';
import { useWeightDecision } from '../hooks/useWeightDecision';
import type { DecisionOption } from './WeightDecisionCards';
import { WeightDecisionCards } from './WeightDecisionCards';

const MAX_REASON_LENGTH = 500;

function errorKey(code: string): string {
  return `shipment.weightDecision.errors.${code === 'INVALID_SHIPMENT_STATUS' ? code : 'unknown'}`;
}

/**
 * Müdür kantar uyuşmazlığında seçenek kartlarından birini seçer: çuval ekle-çıkar, iplik güncelle (reçete sayfasına
 * gider; kilo tartıyı tutunca sevkiyat yola çıkar) ya da iptal. Çuval ve iptal kararında gerekçe zorunludur.
 */
export function WeightDecisionForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const [choice, setChoice] = useState<DecisionOption>('ADD_REMOVE_BAGS');
  const [reason, setReason] = useState('');
  const mutation = useWeightDecision(shipmentId);
  return (
    <SectionCard
      title={t('shipment.weightDecision.title')}
      description={t('shipment.weightDecision.subtitle')}
      icon="discrepancies"
      tone="warning"
      testId="weight-decision"
    >
      <Stack>
        <WeightDecisionCards value={choice} onChange={setChoice} />
        {choice === 'UPDATE_RECIPE' ? (
          <Button
            component={Link}
            to="recete"
            relative="path"
            w="fit-content"
            color="teal"
            leftSection={<Icon name="arrowRight" />}
          >
            {t('shipment.weightDecision.goRecipe')}
          </Button>
        ) : (
          <>
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
              color={choice === 'CANCEL' ? 'red' : 'brand'}
              leftSection={<Icon name="check" />}
              disabled={reason.trim() === ''}
              loading={mutation.isPending}
              onClick={() => {
                mutation.mutate({ decision: choice, reason: reason.trim() });
              }}
            >
              {t('shipment.weightDecision.submit')}
            </Button>
          </>
        )}
      </Stack>
    </SectionCard>
  );
}
