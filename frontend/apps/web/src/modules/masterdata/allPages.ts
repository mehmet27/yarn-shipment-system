const PAGE_SIZE = 100;

/** Seçim listeleri için bütün sayfalar; tanım listeleri küçüktür. */
export async function allPages<T>(
  load: (query: {
    page: number;
    size: number;
  }) => Promise<{ items: T[]; page: { totalPages: number } }>,
): Promise<T[]> {
  const first = await load({ page: 0, size: PAGE_SIZE });
  const rest = await Promise.all(
    Array.from({ length: Math.max(first.page.totalPages - 1, 0) }, (_, i) =>
      load({ page: i + 1, size: PAGE_SIZE }),
    ),
  );
  return [first, ...rest].flatMap((page) => page.items);
}
