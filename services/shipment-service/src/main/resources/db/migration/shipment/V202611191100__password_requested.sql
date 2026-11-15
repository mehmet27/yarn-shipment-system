-- Hatalı girişte hesap sahibi "adminden şifre talep et" der; talep bu sütunda tutulur (boş = açık talep yok).
-- Admin geçici şifre verince, kişi başarıyla girince ya da kendi şifresini değiştirince talep kapanır.
ALTER TABLE app_user ADD COLUMN password_requested_at timestamptz;
