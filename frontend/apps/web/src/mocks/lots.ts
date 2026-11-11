import type { LotView } from '@iplik/api-client';
import { listMasterRecords } from './masterData';

const SEED: readonly LotView[] = [
  {
    id: 'lot-1',
    siteId: 'site-merkez',
    siteName: 'Merkez Depo',
    lotNo: 'P-2026-0001',
    productId: 'prd-0',
    productCode: 'PES-30-1',
    productName: 'Polyester 30/1',
    qualityGradeId: 'grd-a',
    qualityGradeCode: 'A',
    qualityGradeName: 'Birinci kalite',
    yarnSupplierId: 'ysup-0',
    yarnSupplierName: 'Şireci',
    productionDate: '2026-10-01',
    qualityLockedAt: '2026-10-02T08:00:00Z',
    active: true,
  },
  {
    id: 'lot-2',
    siteId: 'site-merkez',
    siteName: 'Merkez Depo',
    lotNo: 'P-2026-0002',
    productId: 'prd-1',
    productCode: 'CO-20-1',
    productName: 'Pamuk 20/1',
    qualityGradeId: 'grd-b',
    qualityGradeCode: 'B',
    qualityGradeName: 'İkinci kalite',
    productionDate: '2026-10-10',
    active: true,
  },
];

let lots: LotView[] = structuredClone([...SEED]);
let nextId = 1;

export type MockLotInput = {
  siteId: string;
  lotNo: string;
  productId: string;
  qualityGradeId: string;
  yarnSupplierId?: string;
  productionDate: string;
};

export function listMockLots(): readonly LotView[] {
  return lots;
}

export function findMockLot(id: string): LotView | undefined {
  return lots.find((lot) => lot.id === id);
}

export function isDuplicateLotNo(siteId: string, lotNo: string, exceptId?: string): boolean {
  return lots.some((lot) => lot.siteId === siteId && lot.lotNo === lotNo && lot.id !== exceptId);
}

function labelOf(kind: 'site' | 'product' | 'qualityGrade' | 'yarnSupplier', id: string) {
  return listMasterRecords(kind).find((record) => record.id === id && record.isActive);
}

/** Lokasyon, iplik ve kalite etkin kayıtlar olmalı (backend `INVALID_MASTER_DATA`). */
export function toMockLot(id: string, input: MockLotInput): LotView | undefined {
  const site = labelOf('site', input.siteId);
  const product = labelOf('product', input.productId);
  const grade = labelOf('qualityGrade', input.qualityGradeId);
  if (site === undefined || product === undefined || grade === undefined) return undefined;
  const supplier =
    input.yarnSupplierId === undefined ? undefined : labelOf('yarnSupplier', input.yarnSupplierId);
  if (input.yarnSupplierId !== undefined && supplier === undefined) return undefined;
  return {
    id,
    siteId: site.id,
    siteName: site.label,
    lotNo: input.lotNo.trim().toUpperCase(),
    productId: product.id,
    productCode: product.code ?? '',
    productName: product.label,
    qualityGradeId: grade.id,
    qualityGradeCode: grade.code ?? '',
    qualityGradeName: grade.label,
    ...(supplier === undefined
      ? {}
      : { yarnSupplierId: supplier.id, yarnSupplierName: supplier.label }),
    productionDate: input.productionDate,
    active: true,
  };
}

export function newMockLotId(): string {
  return `lot-new-${String(nextId++)}`;
}

export function saveMockLot(lot: LotView): void {
  lots = lots.some((current) => current.id === lot.id)
    ? lots.map((current) => (current.id === lot.id ? lot : current))
    : [...lots, lot];
}

export function resetMockLots(): void {
  lots = structuredClone([...SEED]);
  nextId = 1;
}
