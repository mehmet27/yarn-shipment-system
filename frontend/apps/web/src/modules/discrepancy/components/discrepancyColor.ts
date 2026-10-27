import type { DiscrepancyStatus, Severity } from '@iplik/api-client';

/** FE-UI-03: renk tek başına anlam taşımaz; rozet metni de gösterilir. */
export function toSeverityColor(severity: Severity): string {
  return severity === 'RED' ? 'red' : 'yellow';
}

export function toDiscrepancyStatusColor(status: DiscrepancyStatus): string {
  switch (status) {
    case 'OPEN':
      return 'orange';
    case 'IN_REVIEW':
      return 'blue';
    case 'RESOLVED':
      return 'green';
    case 'CLOSED':
      return 'gray';
  }
}
