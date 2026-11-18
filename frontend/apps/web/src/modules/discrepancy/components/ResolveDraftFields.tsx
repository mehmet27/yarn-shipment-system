import { ResolveInvoiceFields } from './ResolveInvoiceFields';
import { ResolveNoteFields } from './ResolveNoteFields';
import type { useResolveDraft } from './useResolveDraft';

/** Seçime göre fatura bilgisi, sorumlu taraf ve not alanları. */
export function ResolveDraftFields({ draft }: { draft: ReturnType<typeof useResolveDraft> }) {
  return (
    <>
      {draft.isInvoiced && (
        <ResolveInvoiceFields value={draft.invoice} onChange={draft.setInvoice} />
      )}
      <ResolveNoteFields
        isPartyRequired={draft.isPartyRequired}
        liableParty={draft.liableParty}
        note={draft.note}
        onLiableParty={draft.setLiableParty}
        onNote={draft.setNote}
      />
    </>
  );
}
