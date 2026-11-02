import type { InternalShipmentView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Paper, Stack, Text } from '@iplik/ui';
import { Can, usePermission } from '@/modules/auth';
import { formatDateTime } from '../components/formatDateTime';
import { QueryState } from '../components/QueryState';
import { useActorLabel } from '../components/useActorLabel';
import { useCorrections } from '../hooks/useCorrections';
import { CorrectionForm } from './CorrectionForm';
import { formatCorrectionValue } from './correctionDraft';

/** Düzeltmeleri Admin (yazar) ve denetim yetkisi olan yönetici (okur) görür. */
export function useCanSeeCorrections(): boolean {
  const canCorrect = usePermission('CORRECTION_CREATE');
  const canAudit = usePermission('AUDIT_VIEW');
  return canCorrect || canAudit;
}

/** Düzeltmeler eskiden yeniye: ne düzeltildi, eski ve yeni değer, gerekçe, kim ve ne zaman. */
export function CorrectionsTab({ shipment }: { shipment: InternalShipmentView }) {
  const { t } = useTranslation();
  const query = useCorrections(shipment.shipmentId);
  const actorLabel = useActorLabel();
  return (
    <Stack>
      <Text c="dimmed" size="sm">
        {t('shipment.correction.intro')}
      </Text>
      <QueryState query={query}>
        {({ items }) =>
          items.length === 0 ? (
            <Text c="dimmed">{t('shipment.correction.empty')}</Text>
          ) : (
            <Stack gap="xs">
              {items.map((item) => (
                <Paper key={item.id} withBorder p="sm">
                  <Text fw={600}>{t(`shipment.correction.targets.${item.target}`)}</Text>
                  <Text size="sm">
                    {formatCorrectionValue(t, item.oldValue)} →{' '}
                    {formatCorrectionValue(t, item.newValue)}
                  </Text>
                  <Text size="sm">{item.reason}</Text>
                  <Text size="sm" c="dimmed">
                    {formatDateTime(item.createdAt)} · {actorLabel(item.requestedBy)}
                  </Text>
                </Paper>
              ))}
            </Stack>
          )
        }
      </QueryState>
      <Can permission="CORRECTION_CREATE">
        <CorrectionForm shipment={shipment} />
      </Can>
    </Stack>
  );
}
