import type { ShipmentStatus } from '@iplik/api-client';

/** FE-UI-03: durum renkle birlikte metinle de verilir. */
export function toStatusColor(status: ShipmentStatus): string {
  switch (status) {
    case 'DRAFT':
      return 'gray';
    case 'LOADED':
    case 'IN_TRANSIT':
      return 'blue';
    case 'AWAITING_DELIVERY':
    case 'CHECKING':
      return 'yellow';
    case 'IN_REVIEW':
      return 'orange';
    case 'COMPLETED':
      return 'green';
    case 'CANCELLED':
      return 'red';
  }
}
