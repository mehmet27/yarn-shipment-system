/** Kumaş girişi: iplik oranları, düşüm önizlemesi, kumaşta kullanılan iplik ve kumaş karışımları. */
import { customInstance } from './customInstance';

const shipmentUrl = (shipmentId: string, path: string) => `/api/v1/shipments/${shipmentId}/${path}`;

/** Kumaş ipliğinin bir sevkiyattan düşülen kilosu. */
export type YarnDraw = { shipmentId: string; shipmentNo?: string; netKg: string };
/** Kumaşın bir ipliği: yüzde payı, kilosu ve hangi sevkiyatlardan düşüldüğü. */
export type FabricYarn = {
  yarnType: string;
  lotId?: string;
  lotNo?: string;
  ratioPct: string;
  netKg: string;
  draws: YarnDraw[];
};
export type FabricReceipt = {
  id: string;
  fabricType: string;
  netKg: string;
  rollCount?: number;
  waybillNo: string;
  receivedAt: string;
  yarns: FabricYarn[];
};
/** Kumaşa yazılan iplik oranı; oran yüzdedir ("33.5"), en çok iki ondalık. */
export type YarnRatio = { yarnType: string; lotId?: string; ratioPct: string };
export type FabricReceiptRequest = {
  fabricType: string;
  netKg: string;
  rollCount?: number;
  waybillNo: string;
  receivedAt?: string;
  yarns: YarnRatio[];
};
export type FabricPreviewRequest = { netKg: string; yarns: YarnRatio[] };
export type YarnShortage = {
  yarnType: string;
  lotId?: string;
  lotNo?: string;
  requiredKg: string;
  missingKg: string;
};
/** Kaydetmeden önce: her iplikten kaç kilo hangi sevkiyattan düşülecek, eksik varsa nerede. */
export type FabricPreview = {
  feasible: boolean;
  yarns: FabricYarn[];
  shortages: YarnShortage[];
};
/** Fasoncudaki bir iplik (parti ve tedarikçisiyle); `ownKg` kumaşın girildiği sevkiyattaki kilodur. */
export type FabricYarnOption = {
  yarnType: string;
  lotId?: string;
  lotNo?: string;
  supplierName?: string;
  totalKg: string;
  ownKg: string;
};
/** Bu sevkiyatın ipliğinden üretilen kumaş (kumaş başka sevkiyattan girilmiş olabilir). */
export type FabricDraw = {
  fabricId: string;
  fabricType: string;
  entryShipmentId: string;
  entryShipmentNo: string;
  yarnType: string;
  lotNo?: string;
  netKg: string;
  receivedAt: string;
};
export type RecipeYarn = { yarnType: string; ratioPct: string };
/** Kumaş cinsinin varsayılan iplik karışımı. */
export type FabricRecipe = {
  id: string;
  fabricType: string;
  yarns: RecipeYarn[];
  active: boolean;
};
export type FabricRecipeRequest = {
  fabricType: string;
  yarns: RecipeYarn[];
  active?: boolean;
};

export const listFabricReceipts = (shipmentId: string) =>
  customInstance<FabricReceipt[]>({
    url: shipmentUrl(shipmentId, 'fabric-receipts'),
    method: 'GET',
  });
export const recordFabricReceipt = (shipmentId: string, data: FabricReceiptRequest) =>
  customInstance<FabricReceipt>({
    url: shipmentUrl(shipmentId, 'fabric-receipts'),
    method: 'POST',
    data,
  });
export const listFabricYarnOptions = (shipmentId: string) =>
  customInstance<FabricYarnOption[]>({
    url: shipmentUrl(shipmentId, 'fabric-receipts/yarn-options'),
    method: 'GET',
  });
export const previewFabricReceipt = (shipmentId: string, data: FabricPreviewRequest) =>
  customInstance<FabricPreview>({
    url: shipmentUrl(shipmentId, 'fabric-receipts/preview'),
    method: 'POST',
    data,
  });
export const listFabricDraws = (shipmentId: string) =>
  customInstance<FabricDraw[]>({
    url: shipmentUrl(shipmentId, 'fabric-receipts/drawn-from-yarn'),
    method: 'GET',
  });

export const FABRIC_RECIPES_PATH = '/api/v1/fabric-recipes';
export const listFabricRecipes = (includeInactive = false) =>
  customInstance<FabricRecipe[]>({
    url: FABRIC_RECIPES_PATH,
    method: 'GET',
    params: includeInactive ? { scope: 'ALL' } : undefined,
  });
export const createFabricRecipe = (data: FabricRecipeRequest) =>
  customInstance<FabricRecipe>({ url: FABRIC_RECIPES_PATH, method: 'POST', data });
export const updateFabricRecipe = (id: string, data: FabricRecipeRequest) =>
  customInstance<FabricRecipe>({ url: `${FABRIC_RECIPES_PATH}/${id}`, method: 'PUT', data });
