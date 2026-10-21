export type MockSubcontractor = { id: string; title: string };
export type MockDriver = { id: string; fullName: string; subcontractorId: string };
export type MockVehicle = { id: string; plateNo: string };
export type MockSite = { id: string; name: string };

export const MOCK_SUBCONTRACTORS: readonly MockSubcontractor[] = [
  { id: 'sub-0', title: 'Aydın Örme' },
  { id: 'sub-1', title: 'Bursa Tekstil' },
  { id: 'sub-2', title: 'Çorlu Boya' },
];

export const MOCK_DRIVERS: readonly MockDriver[] = [
  { id: 'drv-0', fullName: 'Şahin Şoför', subcontractorId: 'sub-0' },
  { id: 'drv-1', fullName: 'Mert Kaya', subcontractorId: 'sub-1' },
  { id: 'drv-2', fullName: 'Ali Demir', subcontractorId: 'sub-1' },
];

export const MOCK_VEHICLES: readonly MockVehicle[] = [
  { id: 'veh-0', plateNo: '34 ABC 123' },
  { id: 'veh-1', plateNo: '16 XYZ 456' },
];

export const MOCK_SITES: readonly MockSite[] = [{ id: 'site-merkez', name: 'Merkez Depo' }];

/** Arama büyük-küçük harf duyarsızdır (Türkçe yerel ayarla). */
export function matchesQuery(text: string, q: string | null): boolean {
  return q === null || text.toLocaleLowerCase('tr').includes(q.toLocaleLowerCase('tr'));
}
