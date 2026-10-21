import type { Permission } from '@iplik/api-client';
import type { ReactNode } from 'react';
import { usePermission } from '../hooks/usePermission';

export type CanProps = {
  permission: Permission;
  children: ReactNode;
  fallback?: ReactNode;
};

export function Can({ permission, children, fallback = null }: CanProps) {
  const isAllowed = usePermission(permission);
  return isAllowed ? children : fallback;
}
