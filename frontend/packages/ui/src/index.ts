// F-05: uygulama kodu Mantine'i yalnızca bu paket üzerinden kullanır.
export {
  Alert,
  Anchor,
  AppShell,
  Badge,
  Burger,
  Button,
  Center,
  Container,
  Group,
  Loader,
  NavLink,
  Pagination,
  Paper,
  Select,
  SimpleGrid,
  Stack,
  Table,
  Tabs,
  Text,
  Title,
} from '@mantine/core';
export { useDisclosure } from '@mantine/hooks';
export { ErrorBoundary } from './ErrorBoundary';
export type { ErrorBoundaryProps } from './ErrorBoundary';
export { reportError } from './reportError';
export { UiProvider } from './UiProvider';
export type { UiProviderProps } from './UiProvider';
