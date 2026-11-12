import type { EvidenceType } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Center, Loader, SimpleGrid, Stack, Text, Title } from '@iplik/ui';
import { useShipmentEvidence } from '../hooks/useEvidenceQueries';
import { EVIDENCE_COLUMNS } from './evidenceColumns';
import { EvidenceItem } from './EvidenceItem';

export type EvidenceGalleryProps = {
  shipmentId: string;
  /** Verilirse yalnızca bu türler gösterilir. */
  types?: readonly EvidenceType[];
  title?: string;
  /** Kayıt yoksa bölümü hiç çizme (başka ekranlara gömülü kullanım). */
  hideWhenEmpty?: boolean;
};

/** Sevkiyatın fotoğraf ve belgeleri; en yeni üstte. */
export function EvidenceGallery({ shipmentId, types, title, hideWhenEmpty }: EvidenceGalleryProps) {
  const { t } = useTranslation();
  const query = useShipmentEvidence(shipmentId);
  if (query.isPending) {
    return (
      <Center py="md">
        <Loader size="sm" />
      </Center>
    );
  }
  if (query.isError) return <Alert color="red">{t('evidence.view.loadFailed')}</Alert>;
  const items = query.data
    .filter((item) => types === undefined || types.includes(item.evidenceType))
    .toSorted((a, b) => b.uploadedAt.localeCompare(a.uploadedAt));
  if (items.length === 0 && hideWhenEmpty === true) return null;
  return (
    <Stack gap="xs">
      {title !== undefined && <Title order={4}>{title}</Title>}
      {items.length === 0 ? (
        <Text c="dimmed">{t('evidence.view.empty')}</Text>
      ) : (
        <SimpleGrid cols={EVIDENCE_COLUMNS}>
          {items.map((item) => (
            <EvidenceItem key={item.id} evidence={item} />
          ))}
        </SimpleGrid>
      )}
    </Stack>
  );
}
