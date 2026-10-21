import { initI18n } from '@iplik/i18n';
import '@iplik/ui/styles.css';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './app/App';
import { isMockApiEnabled } from './app/config';

async function startMockApi(): Promise<void> {
  if (!isMockApiEnabled()) {
    return;
  }
  const { worker } = await import('./mocks/browser');
  await worker.start({ onUnhandledRequest: 'bypass' });
}

async function bootstrap(): Promise<void> {
  await Promise.all([startMockApi(), initI18n()]);
  const container = document.getElementById('root');
  if (container === null) {
    throw new Error('Root element not found');
  }
  createRoot(container).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
}

void bootstrap();
