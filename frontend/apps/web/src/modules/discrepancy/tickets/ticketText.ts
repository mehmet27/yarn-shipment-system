import type { DiscrepancyTicketView } from '@iplik/api-client';
import { formatKg, isKilogram } from '@iplik/units';

/** Backend kiloları gram hassasiyetinde (3 hane) taşır. */
const KG_DECIMALS = 3;

type Translate = (key: string, options: Record<string, string>) => string;

function kg(value: string | undefined): string | undefined {
  if (value === undefined) return undefined;
  return isKilogram(value) ? formatKg(value) : value;
}

/** "Brüt … · Dara … · Net …"; okunamayan fişte boş. */
export function readValues(t: Translate, ticket: DiscrepancyTicketView): string | undefined {
  const { grossKg, tareKg, netKg } = ticket;
  if (grossKg === undefined || tareKg === undefined || netKg === undefined) return undefined;
  return t('ticket.readValues', {
    gross: kg(grossKg) ?? '',
    tare: kg(tareKg) ?? '',
    net: kg(netKg) ?? '',
  });
}

export function enteredNet(ticket: DiscrepancyTicketView): string | undefined {
  return kg(ticket.enteredNetKg);
}

/** Okunan net ile girilen net arasındaki fark, işaretli kg olarak; ikisinden biri yoksa boş. */
export function netDifference(ticket: DiscrepancyTicketView): string | undefined {
  if (ticket.netKg === undefined || ticket.enteredNetKg === undefined) return undefined;
  const diff = Number(ticket.netKg) - Number(ticket.enteredNetKg);
  if (!Number.isFinite(diff)) return undefined;
  const text = kg(Math.abs(diff).toFixed(KG_DECIMALS)) ?? '';
  if (diff > 0) return `+${text}`;
  return diff < 0 ? `−${text}` : text;
}
