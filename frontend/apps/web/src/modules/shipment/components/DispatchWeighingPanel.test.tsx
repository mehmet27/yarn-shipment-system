import type { Weighing } from '@iplik/api-client';
import type { Kilogram } from '@iplik/units';
import { UiProvider } from '@iplik/ui';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DispatchWeighingPanel } from './DispatchWeighingPanel';

const kg = (value: string) => value as Kilogram;

function weighing(extra: Partial<Weighing>): Weighing {
  return {
    grossKg: kg('2000.000'),
    tareKg: kg('18.000'),
    netKg: kg('1982.000'),
    bagCount: 40,
    weighedAt: '2026-10-20T08:15:30Z',
    ...extra,
  };
}

function renderWith(value: Weighing | undefined) {
  return render(
    <UiProvider env="test">
      <DispatchWeighingPanel weighing={value} />
    </UiProvider>,
  );
}

describe('DispatchWeighingPanel', () => {
  it('çuval sayısını gösterir', () => {
    renderWith(weighing({}));
    expect(screen.getByText('40')).toBeInTheDocument();
  });

  it('çıkış tartısı yoksa kaydedilmedi yazar', () => {
    renderWith(undefined);
    expect(screen.getByText('Çıkış tartısı henüz girilmedi')).toBeInTheDocument();
  });
});
