import { useState } from 'react';
import type { ResolveInput } from '../hooks/useDecideDiscrepancy';
import type { ResolveChoice } from './resolveChoices';
import { JUSTIFIED_INVOICED, resolutionOf } from './resolveChoices';
import { emptyInvoice, isInvoiceComplete } from './ResolveInvoiceFields';

/** Karar formunun taslağı: seçim, not, sorumlu taraf ve fatura alanları; gönderilebilir mi, hangi istek. */
export function useResolveDraft() {
  const [choice, setChoiceRaw] = useState<ResolveChoice>();
  const [isConfirming, setConfirming] = useState(false);
  const [note, setNote] = useState('');
  const [liableParty, setLiableParty] = useState('');
  const [invoice, setInvoice] = useState(emptyInvoice);
  const setChoice = (value: ResolveChoice | undefined) => {
    setChoiceRaw(value);
    setConfirming(false);
  };
  const isPartyRequired = choice === 'LIABILITY_ASSIGNED';
  const isInvoiced = choice === JUSTIFIED_INVOICED;
  const canSubmit =
    choice !== undefined &&
    note.trim() !== '' &&
    (!isPartyRequired || liableParty.trim() !== '') &&
    (!isInvoiced || isInvoiceComplete(invoice));
  const toInput = (): ResolveInput | undefined => {
    if (choice === undefined) return undefined;
    const request = {
      resolution: resolutionOf(choice),
      note: note.trim(),
      ...(isPartyRequired ? { liableParty: liableParty.trim() } : {}),
    };
    if (!isInvoiced) return { request };
    const extra = invoice.note.trim() === '' ? {} : { note: invoice.note.trim() };
    return {
      request,
      invoice: { invoiceNo: invoice.invoiceNo.trim(), invoicedOn: invoice.invoicedOn, ...extra },
    };
  };
  return {
    ...{ choice, note, liableParty, invoice, isPartyRequired, isInvoiced, canSubmit, isConfirming },
    ...{ setChoice, setConfirming, setNote, setLiableParty, setInvoice, toInput },
  };
}
