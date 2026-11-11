import type { ShipmentStatus } from '@iplik/api-client';

/** FE-UI-03: durum renkle birlikte metinle de verilir. */
export function toStatusColor(status: ShipmentStatus): string {
  switch (status) {
    case 'DRAFT':
      return 'gray';
    case 'AWAITING_DELIVERY':
      return 'yellow';
    case 'COMPLETED':
      return 'green';
    case 'WEIGHT_MISMATCH':
      return 'orange';
    case 'CANCELLED':
      return 'red';
  }
}
