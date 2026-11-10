import type { ShipmentStatus } from '@iplik/api-client';

/** Sevkiyat sürecinin adımları: taslak (çuval ve çıkış tartısı), fasoncuya yolda, irsaliyeyle tamam. */
export const PROGRESS_STATUSES = ['DRAFT', 'AWAITING_DELIVERY', 'COMPLETED'] as const;

export const PROGRESS_TOTAL_STEPS = PROGRESS_STATUSES.length;

const STEP_AWAITING_DELIVERY = 2;

/** Durumun (kantar uyuşmazlığı taslak adımında bekler); tamamladığı adım sayısı; iptalde süreç ilerlemediği için 0. */
export function toProgressDone(status: ShipmentStatus): number {
  switch (status) {
    case 'DRAFT':
    case 'WEIGHT_MISMATCH':
      return 1;
    case 'AWAITING_DELIVERY':
      return STEP_AWAITING_DELIVERY;
    case 'COMPLETED':
      return PROGRESS_TOTAL_STEPS;
    case 'CANCELLED':
      return 0;
  }
}
