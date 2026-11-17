import { formatKg3 } from '../format';

export function isPositive(kg: string): boolean {
  return Number(kg) > 0;
}

/** Fiyatı girilmemiş kilo varsa kartın altına açıkça yazılır; toplam yanıltmasın. */
export function unpricedText(kg: string, template: (kgText: string) => string): string | undefined {
  return isPositive(kg) ? template(formatKg3(kg)) : undefined;
}
