import { SimpleGrid } from '@iplik/ui';
import { EvidenceById } from './EvidenceById';
import { EVIDENCE_COLUMNS } from './evidenceColumns';

/** İtiraz fotoğrafları gibi kimlik listesi. */
export function EvidenceByIds({ evidenceIds }: { evidenceIds: readonly string[] }) {
  if (evidenceIds.length === 0) return null;
  return (
    <SimpleGrid cols={EVIDENCE_COLUMNS}>
      {evidenceIds.map((id) => (
        <EvidenceById key={id} evidenceId={id} />
      ))}
    </SimpleGrid>
  );
}
