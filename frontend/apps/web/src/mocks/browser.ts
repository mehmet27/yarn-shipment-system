import { setupWorker } from 'msw/browser';
import { createHandlers } from './handlers';
import { MOCK_ROLES } from './users';

/** Geliştirmede rol adres çubuğundan seçilir: /?mockRole=SUBCONTRACTOR (varsayılan ADMIN). */
function readMockRole() {
  const requested = new URLSearchParams(window.location.search).get('mockRole');
  return MOCK_ROLES.find((role) => role === requested) ?? 'ADMIN';
}

export const worker = setupWorker(...createHandlers(readMockRole()));
