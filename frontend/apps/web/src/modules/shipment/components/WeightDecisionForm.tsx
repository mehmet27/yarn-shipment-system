import type { WeightDecision } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Paper, SegmentedControl, Stack, Text, TextInput, Title } from '@iplik/ui';
import { useState } from 'react';
import { useWeightDecision } from '../hooks/useWeightDecision';

const DECISIONS: readonly WeightDecision[] = ['CANCEL', 'ADD_REMOVE_BAGS', 'UPDATE_RECIPE'];
const MAX_REASON_LENGTH = 500;

function errorKey(code: string): string {
  return `shipment.weightDecision.errors.${code === 'INVALID_SHIPMENT_STATUS' ? code : 'unknown'}`;
}

/** Müdür ve yönetici: kantar uyuşmazlığında iptal, çuval ekle-çıkar ya da reçeteyi güncelle; gerekçe zorunlu. */
export function WeightDecisionForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const [decision, setDecision] = useState<WeightDecision>('ADD_REMOVE_BAGS');
  const [reason, setReason] = useState('');
  const mutation = useWeightDecision(shipmentId);
  return (
    <Paper withBorder p="md" radius="md" data-testid="weight-decision">
      <Stack>
        <Title order={2} size="h4">
          {t('shipment.weightDecision.title')}
        </Title>
        <SegmentedControl
          fullWidth
          orientation="vertical"
          aria-label={t('shipment.weightDecision.decision')}
          value={decision}
          data={DECISIONS.map((value) => ({
            value,
            label: t(`shipment.weightDecision.options.${value}`),
          }))}
          onChange={(value) => {
            setDecision(DECISIONS.find((candidate) => candidate === value) ?? decision);
          }}
        />
        <Text size="sm" c="dimmed">
          {t(`shipment.weightDecision.hints.${decision}`)}
        </Text>
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
          disabled={reason.trim() === ''}
          loading={mutation.isPending}
          onClick={() => {
            mutation.mutate({ decision, reason: reason.trim() });
          }}
        >
          {t('shipment.weightDecision.submit')}
        </Button>
      </Stack>
    </Paper>
  );
}
