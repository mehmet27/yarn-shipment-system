import { ResolveNoteFields } from './ResolveNoteFields';
import type { useResolveDraft } from './useResolveDraft';

/** Karar notu alanı. */
export function ResolveDraftFields({ draft }: { draft: ReturnType<typeof useResolveDraft> }) {
  return <ResolveNoteFields note={draft.note} onNote={draft.setNote} />;
}
