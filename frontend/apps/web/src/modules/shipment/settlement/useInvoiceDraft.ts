import type { FireSettlement } from '@iplik/api-client/settlements';
import { useState } from 'react';
import { useMarkInvoiced } from './hooks';
import type { DraftError, InvoiceLineDraft } from './settlementDraft';
import { toInvoiceRequest } from './settlementDraft';

/** Bugünün tarihi (İstanbul saatiyle), `YYYY-MM-DD`; İsveççe yerel ayar bu biçimi verir. */
const DATE_FORMAT = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Europe/Istanbul' });
const todayIso = () => DATE_FORMAT.format(new Date());
const KG_TEXT = new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 3 });

/** Tek iplikli sevkiyatta kalan kilo hazır yazılır; birden çok iplikte müdür iplik iplik yazar. */
function initialLines(settlement: FireSettlement): InvoiceLineDraft[] {
  const isSingle = settlement.yarns.length === 1;
  return settlement.yarns.map((yarn) => ({
    yarnType: yarn.yarnType,
    netKg: isSingle ? KG_TEXT.format(Number(settlement.remainingKg)) : '',
  }));
}

/** Fatura formunun alanları, iplik satırları ve gönderimi. */
export function useInvoiceDraft(settlement: FireSettlement) {
  const [invoiceNo, setInvoiceNo] = useState('');
  const [invoicedOn, setInvoicedOn] = useState(todayIso);
  const [note, setNote] = useState('');
  const [lines, setLines] = useState(() => initialLines(settlement));
  const [error, setError] = useState<DraftError>();
  const mutation = useMarkInvoiced(settlement.shipmentId);
  const submit = () => {
    const request = toInvoiceRequest(
      { invoiceNo, invoicedOn, note, lines },
      settlement.remainingKg,
    );
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') mutation.mutate(request);
  };
  const setLine = (index: number, netKg: string) => {
    setLines(lines.map((line, i) => (i === index ? { ...line, netKg } : line)));
  };
  return {
    fields: { invoiceNo, invoicedOn, note, lines },
    set: { invoiceNo: setInvoiceNo, invoicedOn: setInvoicedOn, note: setNote, line: setLine },
    error,
    mutation,
    submit,
  };
}
