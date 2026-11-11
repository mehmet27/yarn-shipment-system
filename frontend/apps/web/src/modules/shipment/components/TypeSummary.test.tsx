import { UiProvider } from '@iplik/ui';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { TypeSummary } from './TypeSummary';

function renderWith(types: string[]) {
  return render(
    <UiProvider env="test">
      <TypeSummary types={types} />
    </UiProvider>,
  );
}

describe('TypeSummary', () => {
  it('ambalajları türe göre sayar', () => {
    renderWith(['BAG', 'BAG', 'BAG']);

    expect(screen.getByText('Türe göre: 3 Çuval')).toBeInTheDocument();
  });

  it('tür bilgisi yoksa hiçbir şey çizmez', () => {
    renderWith([]);

    expect(screen.queryByText(/Türe göre/)).not.toBeInTheDocument();
  });
});
