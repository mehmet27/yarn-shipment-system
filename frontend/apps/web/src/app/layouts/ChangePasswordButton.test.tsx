import { changePasswordUrl } from '@iplik/api-client';
import { UiProvider } from '@iplik/ui';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ChangePasswordButton } from './ChangePasswordButton';

describe('ChangePasswordButton', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('Keycloak şifre değiştirme eylemine gider; şifre bu uygulamada sorulmaz', async () => {
    const assign = vi.fn();
    vi.stubGlobal('location', { assign });
    render(
      <UiProvider env="test">
        <ChangePasswordButton />
      </UiProvider>,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Şifremi değiştir' }));

    expect(assign).toHaveBeenCalledWith(changePasswordUrl());
    expect(changePasswordUrl()).toContain('kc_action=UPDATE_PASSWORD');
  });
});
