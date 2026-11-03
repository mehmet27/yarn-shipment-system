import type { DiscrepancyTicketView, TicketMisreadView } from '@iplik/api-client';

/** Miktar uyuşmazlığında çıkış fişi okunmuş, teslim fişi okunamamış (backend TicketReviews). */
const READ_AT = '2026-10-20T08:05:00Z';
const misreads = new Map<string, TicketMisreadView>();

function evidenceIdsOf(discrepancyId: string): readonly [string, string] {
  return [`${discrepancyId}-dispatch-ticket`, `${discrepancyId}-delivery-ticket`];
}

export function mockTickets(discrepancyId: string, canReview: boolean): DiscrepancyTicketView[] {
  const [dispatchId, deliveryId] = evidenceIdsOf(discrepancyId);
  const tickets: Omit<DiscrepancyTicketView, 'allowedActions'>[] = [
    {
      evidenceId: deliveryId,
      side: 'DELIVERY_SCALE',
      status: 'UNREADABLE',
      enteredNetKg: '1960.000',
      problems: ['WEIGHT_NOT_READ', 'BLURRY'],
      readAt: READ_AT,
    },
    {
      evidenceId: dispatchId,
      side: 'DISPATCH_SCALE',
      status: 'READ',
      grossKg: '4500.000',
      tareKg: '2500.000',
      netKg: '2000.000',
      enteredNetKg: '1990.000',
      plate: '34 ABC 123',
      ticketNo: 'F-1001',
      scaleName: 'Merkez kantar',
      problems: [],
      readAt: READ_AT,
    },
  ];
  return tickets.map((ticket) => {
    const misread = misreads.get(ticket.evidenceId);
    return {
      ...ticket,
      ...(misread === undefined ? {} : { misread }),
      allowedActions: canReview && misread === undefined ? ['MARK_MISREAD'] : [],
    };
  });
}

/** İlk işaret kalır; aynı okumaya ikinci işaret değişiklik yapmaz. */
export function markMockMisread(evidenceId: string, userId: string): boolean {
  if (!evidenceId.endsWith('-ticket')) return false;
  if (!misreads.has(evidenceId)) {
    misreads.set(evidenceId, { markedBy: userId, markedAt: new Date().toISOString() });
  }
  return true;
}
