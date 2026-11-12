import type { EvidenceView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Badge, Group, Paper, Stack, Text } from '@iplik/ui';
import { EvidenceFileLink } from './EvidenceFileLink';
import { EvidenceImageThumb } from './EvidenceImageThumb';

const isImage = (mimeType: string) => mimeType.startsWith('image/');

/** Tek kanıt: önizleme (fotoğraf) ya da bağlantı (PDF), tür, saat ve geçersiz kılınmışsa gerekçe. */
export function EvidenceItem({ evidence }: { evidence: EvidenceView }) {
  const { t } = useTranslation();
  const isInvalid = evidence.status === 'INVALIDATED';
  return (
    <Paper withBorder p="sm" aria-label={t(`evidence.types.${evidence.evidenceType}`)}>
      <Stack gap="xs">
        <Group justify="space-between" wrap="nowrap">
          <Text fw={600} size="sm">
            {t(`evidence.types.${evidence.evidenceType}`)}
          </Text>
          {isInvalid && (
            <Badge color="red" variant="light">
              {t('evidence.view.invalidated')}
            </Badge>
          )}
        </Group>
        <div style={isInvalid ? { opacity: 0.5 } : undefined}>
          {isImage(evidence.mimeType) ? (
            <EvidenceImageThumb evidence={evidence} />
          ) : (
            <EvidenceFileLink evidence={evidence} />
          )}
        </div>
        <Text size="xs" c="dimmed">
          {formatDateTime(evidence.uploadedAt)}
        </Text>
        {isInvalid && evidence.invalidatedReason !== undefined && (
          <Text size="xs" c="red">
            {t('evidence.view.reason')}: {evidence.invalidatedReason}
          </Text>
        )}
      </Stack>
    </Paper>
  );
}
