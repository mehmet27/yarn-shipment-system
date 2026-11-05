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
  Checkbox,
  ColorInput,
  Container,
  FileButton,
  Group,
  List,
  Loader,
  Modal,
  NavLink,
  NumberInput,
  Pagination,
  Paper,
  PasswordInput,
  SegmentedControl,
  Select,
  SimpleGrid,
  Stack,
  Stepper,
  Switch,
  Table,
  Tabs,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
export { Box } from '@mantine/core';
export { useDebouncedValue, useDisclosure, useMediaQuery } from '@mantine/hooks';
export { ErrorBoundary } from './ErrorBoundary';
export type { ErrorBoundaryProps } from './ErrorBoundary';
export { reportError } from './reportError';
export { UiProvider } from './UiProvider';
export type { UiProviderProps } from './UiProvider';
export {
  BRAND_SHADE_INDEX,
  DEFAULT_BRAND_COLOR,
  createBrandPalette,
  isValidHexColor,
} from './brandPalette';
export { createAppTheme } from './theme';
export { ActionBar } from './components/ActionBar';
export type { ActionBarProps } from './components/ActionBar';
export { ActivityFeed } from './components/ActivityFeed';
export type { ActivityFeedProps, ActivityItem } from './components/ActivityFeed';
export { EmptyState } from './components/EmptyState';
export type { EmptyStateProps } from './components/EmptyState';
export { ListGroupHeader } from './components/ListGroupHeader';
export type { ListGroupHeaderProps } from './components/ListGroupHeader';
export { MiniProgress } from './components/MiniProgress';
export type { MiniProgressProps } from './components/MiniProgress';
export { PageHero } from './components/PageHero';
export type { PageHeroProps } from './components/PageHero';
export { ProgressSteps } from './components/ProgressSteps';
export type { ProgressStepsProps } from './components/ProgressSteps';
export { StatCard } from './components/StatCard';
export type { StatCardProps, StatTone } from './components/StatCard';
export { BRAND_GRADIENT, BRAND_SIDEBAR_GRADIENT } from './brandGradient';
export { NavIcon } from './components/NavIcon';
export type { NavIconName, NavIconProps } from './components/NavIcon';
