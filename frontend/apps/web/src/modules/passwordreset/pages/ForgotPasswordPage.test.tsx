import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';

async function requestCode(username: string) {
  await userEvent.type(await screen.findByLabelText(/Giriş adı/), username);
  await userEvent.click(screen.getByRole('button', { name: 'Kod gönder' }));
}

describe('Şifremi unuttum', () => {
  it('giriş yapmadan açılır; kod istenir, kodla yeni şifre belirlenir ve giriş bağlantısı çıkar', async () => {
    renderApp('/sifremi-unuttum');
    await requestCode('ali.yilmaz');

    await userEvent.type(await screen.findByLabelText(/SMS kodu/), '123456');
    await userEvent.type(screen.getByLabelText(/Yeni şifre/), 'Yeni-Sifre-2026');
    await userEvent.click(screen.getByRole('button', { name: 'Şifreyi belirle' }));

    expect(await screen.findByText(/Şifreniz belirlendi/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Giriş yap' })).toHaveAttribute(
      'href',
      '/oauth2/authorization/keycloak',
    );
  });

  it('yanlış kod ve kısa şifre için ne yapılacağını söyler', async () => {
    renderApp('/sifremi-unuttum');
    await requestCode('ali.yilmaz');

    await userEvent.type(await screen.findByLabelText(/SMS kodu/), '000000');
    await userEvent.type(screen.getByLabelText(/Yeni şifre/), 'kisa');
    await userEvent.click(screen.getByRole('button', { name: 'Şifreyi belirle' }));
    expect(await screen.findByText('Şifre en az 8 karakter olmalı.')).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText(/Yeni şifre/), 'uzunSifre1');
    await userEvent.click(screen.getByRole('button', { name: 'Şifreyi belirle' }));
    expect(await screen.findByText(/Kod hatalı, süresi dolmuş/)).toBeInTheDocument();
  });

  it('kod hatalıysa gelen ileti, kod ya da şifre düzeltilince kaybolur', async () => {
    renderApp('/sifremi-unuttum');
    await requestCode('ali.yilmaz');

    await userEvent.type(await screen.findByLabelText(/SMS kodu/), '000000');
    await userEvent.type(screen.getByLabelText(/Yeni şifre/), 'uzunSifre1');
    await userEvent.click(screen.getByRole('button', { name: 'Şifreyi belirle' }));
    expect(await screen.findByText(/Kod hatalı, süresi dolmuş/)).toBeInTheDocument();

    await userEvent.clear(screen.getByLabelText(/SMS kodu/));
    await userEvent.type(screen.getByLabelText(/SMS kodu/), '1');
    expect(screen.queryByText(/Kod hatalı, süresi dolmuş/)).not.toBeInTheDocument();
  });

  it('giriş adı boşsa istek gitmez', async () => {
    renderApp('/sifremi-unuttum');
    await userEvent.click(await screen.findByRole('button', { name: 'Kod gönder' }));
    expect(screen.getByText('Giriş adını yazın.')).toBeInTheDocument();
  });
});
