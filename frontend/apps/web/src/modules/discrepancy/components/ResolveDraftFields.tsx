import { ResolveNoteFields } from './ResolveNoteFields';
import type { useResolveDraft } from './useResolveDraft';

/** Seçime göre sorumlu taraf ve not alanları. */
export function ResolveDraftFields({ draft }: { draft: ReturnType<typeof useResolveDraft> }) {
  return (
    <ResolveNoteFields
      isPartyRequired={draft.isPartyRequired}
      liableParty={draft.liableParty}
      note={draft.note}
      onLiableParty={draft.setLiableParty}
      onNote={draft.setNote}
    />
  );
}
