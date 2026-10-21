// F-05: uygulama kodu Mantine'i yalnızca bu paket üzerinden kullanır.
export {
  Alert,
  Anchor,
  AppShell,
  Badge,
  Burger,
  CloseButton,
  Button,
  Center,
  Container,
  Group,
  List,
  Loader,
  NavLink,
  Pagination,
  Paper,
  SegmentedControl,
  Select,
  SimpleGrid,
  Stack,
  Stepper,
  Table,
  Tabs,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
export { useDisclosure } from '@mantine/hooks';
export { ErrorBoundary } from './ErrorBoundary';
export type { ErrorBoundaryProps } from './ErrorBoundary';
export { reportError } from './reportError';
export { UiProvider } from './UiProvider';
export type { UiProviderProps } from './UiProvider';
