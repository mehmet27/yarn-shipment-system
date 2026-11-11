import {
  activateProduct,
  activateQualityGrade,
  activateYarnSupplier,
  createProduct,
  createQualityGrade,
  createYarnSupplier,
  deactivateProduct,
  deactivateQualityGrade,
  deactivateYarnSupplier,
  listProducts,
  listQualityGrades,
  listYarnSuppliers,
  updateProduct,
  updateQualityGrade,
  updateYarnSupplier,
} from '@iplik/api-client';
import type { ListParams, RecordPage, RecordValues } from './kinds';

export type Adapter = {
  list: (params: ListParams) => Promise<RecordPage>;
  save: (id: string | undefined, values: RecordValues) => Promise<void>;
  setActive: (id: string, isActive: boolean) => Promise<void>;
};

/** Sözleşmedeki ortak sorgu: pasifler yalnızca yönetici için `status=ALL` ile gelir. */
export function toQuery({ q, page, size, isShowingInactive }: ListParams) {
  return {
    page,
    size,
    status: isShowingInactive ? ('ALL' as const) : ('ACTIVE' as const),
    ...(q === '' ? {} : { q }),
  };
}

export async function discard(request: Promise<unknown>): Promise<void> {
  await request;
}

export const product: Adapter = {
  async list(params) {
    const page = await listProducts(toQuery(params));
    return {
      rows: page.items.map((row) => ({
        id: row.id,
        code: row.code,
        label: row.name,
        isActive: row.active ?? true,
        ...(row.yarnCount === undefined ? {} : { extra: row.yarnCount }),
      })),
      totalPages: page.page.totalPages,
    };
  },
  save(id, { code, label, extra }) {
    const body = { name: label, ...(extra === undefined ? {} : { yarnCount: extra }) };
    return discard(
      id === undefined ? createProduct({ code: code ?? '', ...body }) : updateProduct(id, body),
    );
  },
  setActive: (id, isActive) => discard(isActive ? activateProduct(id) : deactivateProduct(id)),
};

export const qualityGrade: Adapter = {
  async list(params) {
    const page = await listQualityGrades(toQuery(params));
    return {
      rows: page.items.map((row) => ({
        id: row.id,
        code: row.code,
        label: row.name,
        isActive: row.active ?? true,
        ...(row.labelColor === undefined ? {} : { extra: row.labelColor }),
      })),
      totalPages: page.page.totalPages,
    };
  },
  save(id, { code, label, extra }) {
    const body = { name: label, ...(extra === undefined ? {} : { labelColor: extra }) };
    return discard(
      id === undefined
        ? createQualityGrade({ code: code ?? '', ...body })
        : updateQualityGrade(id, body),
    );
  },
  setActive: (id, isActive) =>
    discard(isActive ? activateQualityGrade(id) : deactivateQualityGrade(id)),
};

export const yarnSupplier: Adapter = {
  async list(params) {
    const page = await listYarnSuppliers(toQuery(params));
    return {
      rows: page.items.map((row) => ({
        id: row.id,
        label: row.name,
        isActive: row.active ?? true,
      })),
      totalPages: page.page.totalPages,
    };
  },
  save: (id, { label }) =>
    discard(
      id === undefined
        ? createYarnSupplier({ name: label })
        : updateYarnSupplier(id, { name: label }),
    ),
  setActive: (id, isActive) =>
    discard(isActive ? activateYarnSupplier(id) : deactivateYarnSupplier(id)),
};
