import { Component } from 'react';
import type { ReactNode } from 'react';
import { reportError } from './reportError';

export type ErrorBoundaryProps = {
  fallback: ReactNode;
  children: ReactNode;
};

type ErrorBoundaryState = { hasError: boolean };

/** FE-REACT-01: sınıf bileşeninin izinli tek kullanımı. */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  override state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  override componentDidCatch(error: Error): void {
    reportError(error);
  }

  override render(): ReactNode {
    return this.state.hasError ? this.props.fallback : this.props.children;
  }
}
