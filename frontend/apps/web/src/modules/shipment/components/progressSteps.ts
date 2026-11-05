import type { ShipmentStatus } from '@iplik/api-client';

/** Sevkiyat sürecindeki adım sayısı: taslak, yükleme, yol, teslim, kontrol, tamam. */
export const PROGRESS_TOTAL_STEPS = 6;

const STEP_LOADED = 2;
const STEP_IN_TRANSIT = 3;
const STEP_AWAITING_DELIVERY = 4;
const STEP_CHECKING = 5;

/** Durumun tamamladığı adım sayısı; iptalde süreç ilerlemediği için 0. */
export function toProgressDone(status: ShipmentStatus): number {
  switch (status) {
    case 'DRAFT':
      return 1;
    case 'LOADED':
      return STEP_LOADED;
    case 'IN_TRANSIT':
      return STEP_IN_TRANSIT;
    case 'AWAITING_DELIVERY':
      return STEP_AWAITING_DELIVERY;
    case 'CHECKING':
    case 'IN_REVIEW':
      return STEP_CHECKING;
    case 'COMPLETED':
      return PROGRESS_TOTAL_STEPS;
    case 'CANCELLED':
      return 0;
  }
}
