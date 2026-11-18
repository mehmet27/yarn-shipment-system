import { setupServer } from 'msw/node';
import { createAssistantHandlers } from './assistantHandlers';
import { createHandlers } from './handlers';

export const server = setupServer(...createHandlers('ADMIN'), ...createAssistantHandlers());
