import { RequirePermission } from '@/modules/auth';
import type { RouteObject } from 'react-router';
import { BagLabelsPage } from './pages/BagLabelsPage';

/** /panel altındaki çuval ve etiket route'u. */
export const bagPanelRoutes: RouteObject[] = [
  {
    path: 'cuvallar',
    element: (
      <RequirePermission permission="BAG_CREATE">
        <BagLabelsPage />
      </RequirePermission>
    ),
  },
];
