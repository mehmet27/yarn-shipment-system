import type { RecipeLineView } from '@iplik/api-client';
import { UiProvider } from '@iplik/ui';
import type { Kilogram } from '@iplik/units';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { RecipePanel } from './RecipePanel';

const kg = (value: string) => value as Kilogram;
const ID = '5ab0deca-6d2a-4729-a66b-0d9fed37ed15';

function renderPanel(lines: RecipeLineView[]) {
  render(
    <UiProvider>
      <RecipePanel lines={lines} fireRate={undefined} />
    </UiProvider>,
  );
}

describe('RecipePanel', () => {
  it('iplik ve lotu kimlik yerine adı ve numarasıyla gösterir', () => {
    renderPanel([
      {
        lineNo: 1,
        yarnTypeId: ID,
        yarnTypeName: 'Polyester 30/1',
        lotId: ID,
        lotNo: 'P-0001',
        netKg: kg('1050.000'),
      },
    ]);
    expect(screen.getByText('Polyester 30/1')).toBeInTheDocument();
    expect(screen.getByText('P-0001')).toBeInTheDocument();
    expect(screen.queryByText(ID)).not.toBeInTheDocument();
  });

  it('ad gelmezse eski gösterime döner', () => {
    renderPanel([{ lineNo: 1, yarnTypeText: '36/1 Viskon', netKg: kg('10.000') }]);
    expect(screen.getByText('36/1 Viskon')).toBeInTheDocument();
  });
});
