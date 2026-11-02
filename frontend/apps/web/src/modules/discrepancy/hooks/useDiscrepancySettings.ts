import type { UpdateDiscrepancySettingsRequest } from '@iplik/api-client';
import {
  DISCREPANCY_SETTINGS_PATH,
  getDiscrepancySettings,
  updateDiscrepancySettings,
} from '@iplik/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

export function useDiscrepancySettings() {
  return useQuery({
    queryKey: [DISCREPANCY_SETTINGS_PATH],
    queryFn: getDiscrepancySettings,
  });
}

/** Kayıttan dönen güncel ayarlar önbelleğe yazılır; geçmiş tablosu yeniden istek atmadan güncellenir. */
export function useUpdateDiscrepancySettings() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: UpdateDiscrepancySettingsRequest) => updateDiscrepancySettings(request),
    onSuccess: (settings) => {
      queryClient.setQueryData([DISCREPANCY_SETTINGS_PATH], settings);
    },
  });
}
