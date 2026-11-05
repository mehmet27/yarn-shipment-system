import type { Weighing } from '@iplik/api-client';
import type { Kilogram } from '@iplik/units';
import { UiProvider } from '@iplik/ui';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { WeightComparison } from './WeightComparison';

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

function renderWith(dispatchWeighing: Weighing, dispatchHidden = false) {
  return render(
    <UiProvider env="test">
      <WeightComparison
        weighings={{ dispatchWeighing, hidden: { dispatch: dispatchHidden, delivery: false } }}
      />
    </UiProvider>,
  );
}

describe('WeightComparison dara sapması', () => {
  it('sınır aşıldığında sapmayı ve uyarı rozetini gösterir', () => {
    renderWith(
      weighing({
        expectedTareKg: kg('20.000'),
        tareTolerancePct: '5.00',
        tareDeviationPct: '-10.00',
        tareAlert: true,
      }),
    );
    expect(screen.getByText('Dara sapması')).toBeInTheDocument();
    expect(screen.getByText('-%10')).toBeInTheDocument();
    expect(screen.getByText('Standart dışı')).toBeInTheDocument();
  });

  it('sınır içindeyse rozet göstermez', () => {
    renderWith(
      weighing({
        expectedTareKg: kg('18.000'),
        tareTolerancePct: '5.00',
        tareDeviationPct: '1.50',
        tareAlert: false,
      }),
    );
    expect(screen.getByText('%1,5')).toBeInTheDocument();
    expect(screen.queryByText('Standart dışı')).not.toBeInTheDocument();
  });

  it('standart tanımsızsa sapma satırı yoktur', () => {
    renderWith(weighing({}));
    expect(screen.queryByText('Dara sapması')).not.toBeInTheDocument();
  });
});
