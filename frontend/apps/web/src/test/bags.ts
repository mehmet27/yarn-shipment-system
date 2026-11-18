import { assignMockBagWeights } from '@/mocks/mockStore';

const SHIPMENT_ID = 'shp-0001';
const LOADED_BAGS = 40;
const STANDARD_BAG_KG = '49.500';

/** shp-0001 için 40 çuval ekler; toplam 1.982,5 kg (2.000 − 17,5 tartısına uyar). */
export function addBagsTotalling1982Point5(): void {
  assignMockBagWeights(SHIPMENT_ID, [
    ...Array<string>(LOADED_BAGS - 1).fill(STANDARD_BAG_KG),
    '52.000',
  ]);
}

/** shp-0001 için 40 çuval ekler; toplam 1.972,5 kg (1.990 − 17,5 tartısına uyar). */
export function addBagsTotalling1972Point5(): void {
  assignMockBagWeights(SHIPMENT_ID, [...Array<string>(LOADED_BAGS - 1).fill('49.000'), '61.500']);
}
