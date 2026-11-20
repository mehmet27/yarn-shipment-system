#!/usr/bin/env bash
# deploy/prod kurulumunu uçtan uca dener: install.sh → giriş → yedekle.sh → her şeyi sil → install.sh → geri-yukle.sh → doğrula.
# CI'da (.github/workflows/deploy.yml) ve Docker'lı bir Linux makinede elle çalışır; 80 ve 443 portları boş olmalıdır.
# GERÇEK SUNUCUDA ÇALIŞTIRMAYIN: deploy/prod yığınını ve verisini siler. deploy/prod/.env varsa durur.
#
# Kullanım (root ile):  tools/ci/deploy-smoke.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PROD="$ROOT/deploy/prod"
ENV="$PROD/.env"
YEDEK="$(mktemp -d)"
COMPOSE=(docker compose -f "$PROD/docker-compose.yml")
[ ! -f "$PROD/docker-compose.override.yml" ] || COMPOSE+=(-f "$PROD/docker-compose.override.yml")
COMPOSE+=(--env-file "$ENV")
ADRES=https://localhost
YENI_SIFRE='Duman-Testi!4471q'
CEREZ="$(mktemp)"

[ ! -e "$ENV" ] || { echo "HATA: $ENV var. Bu betik yalnızca boş bir deneme ortamında çalışır." >&2; exit 1; }

temizle() {
  local kod=$?
  if [ "$kod" != 0 ]; then
    echo; echo "=== DUMAN TESTİ BAŞARISIZ (kod $kod); servis günlüklerinin sonu:"
    "${COMPOSE[@]}" logs --tail 80 2>/dev/null || true
  fi
  "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
  [ "$(id -u)" != 0 ] || "$PROD/yedek-kur.sh" --kaldir >/dev/null 2>&1 || true
  rm -rf "$ENV" "$YEDEK" "$CEREZ"
}
trap temizle EXIT

adim() { echo; echo "=== $*"; }
# Caddy "localhost" için kendi iç sertifikasını üretir; duman testi sertifikayı doğrulamaz.
istek() { curl -ksS -b "$CEREZ" -c "$CEREZ" "$@"; }
# Tarayıcı gibi: XSRF-TOKEN çerezi varsa değeri X-XSRF-TOKEN başlığıyla geri gönderilir.
csrf() { awk '$6 == "XSRF-TOKEN" { print $7 }' "$CEREZ" | tail -1; }
giris() {
  : > "$CEREZ"
  istek -o /dev/null "$ADRES/api/v1/branding"
  istek -o /dev/null -w '%{http_code}' -X POST "$ADRES/api/v1/auth/login" \
    -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $(csrf)" \
    -d "{\"username\":\"admin\",\"password\":\"$1\"}"
}
bekle_kod() {
  local beklenen="$1" gercek="$2" ne="$3"
  [ "$beklenen" = "$gercek" ] || { echo "HATA: $ne → beklenen $beklenen, gelen $gercek" >&2; exit 1; }
  echo "ok: $ne ($gercek)"
}
kur() {
  cp "$PROD/.env.example" "$ENV"
  sed -i "s|^APP_HOST=.*|APP_HOST=localhost|; s|^ACME_EMAIL=.*|ACME_EMAIL=duman@test.invalid|; s|^YEDEK_DIZINI=.*|YEDEK_DIZINI=$YEDEK|" "$ENV"
  "$PROD/install.sh"
}
env_deger() { grep -E "^$1=" "$ENV" | cut -d= -f2-; }

adim "1. Temiz kurulum (install.sh)"
kur
ILK_SIFRE="$(env_deger INITIAL_ADMIN_PASSWORD)"

adim "2. Arayüz ve API dışarıdan (Caddy → nginx → uygulama) yanıt veriyor mu"
for _ in $(seq 1 30); do
  [ "$(curl -ks -o /dev/null -w '%{http_code}' "$ADRES/")" = 200 ] && break
  sleep 2
done
bekle_kod 200 "$(istek -o /dev/null -w '%{http_code}' "$ADRES/")" "arayüz ana sayfası"
bekle_kod 200 "$(istek -o /dev/null -w '%{http_code}' "$ADRES/panel/sevkiyat")" "tek sayfa uygulaması yolu index.html'e düşer"
bekle_kod 200 "$(istek -o /dev/null -w '%{http_code}' "$ADRES/api/v1/branding")" "herkese açık API"
bekle_kod 401 "$(istek -o /dev/null -w '%{http_code}' "$ADRES/api/v1/me")" "oturumsuz istek reddedilir"
bekle_kod 308 "$(curl -s -o /dev/null -w '%{http_code}' http://localhost/)" "HTTP, HTTPS'e yönlenir"

adim "3. admin ilk şifreyle girer ve şifresini değiştirir"
bekle_kod 200 "$(giris "$ILK_SIFRE")" "admin ilk şifreyle giriş"
grep -q 'YARNSHIPMENT_SESSION' "$CEREZ" || { echo "HATA: oturum çerezi gelmedi" >&2; exit 1; }
awk '$6 == "YARNSHIPMENT_SESSION" && $4 != "TRUE" { exit 1 }' "$CEREZ" || { echo "HATA: oturum çerezi Secure değil" >&2; exit 1; }
echo "ok: oturum çerezi Secure"
bekle_kod 204 "$(istek -o /dev/null -w '%{http_code}' -X PUT "$ADRES/api/v1/auth/password" \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $(csrf)" \
  -d "{\"currentPassword\":\"$ILK_SIFRE\",\"newPassword\":\"$YENI_SIFRE\"}")" "şifre değiştirme"
bekle_kod 200 "$(istek -o /dev/null -w '%{http_code}' "$ADRES/api/v1/me")" "oturum açık"

adim "4. Kanıt klasörüne örnek dosya ve yedek (yedekle.sh)"
KANIT="$(docker volume inspect -f '{{ .Mountpoint }}' yarn-shipment-prod_evidence-data)"
mkdir -p "$KANIT/evidence/duman"
echo "duman testi kanıtı" > "$KANIT/evidence/duman/ornek.txt"
"$PROD/yedekle.sh"
[ -f "$YEDEK/son-basarili-yedek.txt" ] || { echo "HATA: son-basarili-yedek.txt yazılmadı" >&2; exit 1; }
[ -f "$YEDEK/evidence/evidence/duman/ornek.txt" ] || { echo "HATA: kanıt dosyası yedeğe girmedi" >&2; exit 1; }
"$PROD/durum.sh" || echo "(durum.sh uyarı verdi; duman testinde gece yedeği kurulu olmayabilir)"

adim "5. Güncelleme: install.sh yeniden çalışır, veri korunur, önce döküm alınır"
ONCE="$(find "$YEDEK/veritabani" -name '*.dump' | wc -l)"
"$PROD/install.sh"
SONRA="$(find "$YEDEK/veritabani" -name '*.dump' | wc -l)"
[ "$SONRA" -gt "$ONCE" ] || { echo "HATA: güncelleme öncesi döküm alınmadı" >&2; exit 1; }
bekle_kod 200 "$(giris "$YENI_SIFRE")" "güncellemeden sonra değiştirilen şifreyle giriş"

adim "6. Felaket: tüm veri silinir, sıfırdan kurulur"
"${COMPOSE[@]}" down -v --remove-orphans
rm -f "$ENV"
kur
bekle_kod 401 "$(giris "$YENI_SIFRE")" "boş kurulumda eski şifre geçmez"

adim "7. Geri yükleme (geri-yukle.sh)"
"$PROD/geri-yukle.sh" --evet
for _ in $(seq 1 30); do
  [ "$(curl -ks -o /dev/null -w '%{http_code}' "$ADRES/api/v1/branding")" = 200 ] && break
  sleep 2
done
bekle_kod 200 "$(giris "$YENI_SIFRE")" "geri yüklemeden sonra yedekteki şifreyle giriş"
KANIT="$(docker volume inspect -f '{{ .Mountpoint }}' yarn-shipment-prod_evidence-data)"
[ "$(cat "$KANIT/evidence/duman/ornek.txt")" = "duman testi kanıtı" ] || { echo "HATA: kanıt dosyası geri gelmedi" >&2; exit 1; }
[ "$(stat -c %u "$KANIT/evidence/duman/ornek.txt")" = 10001 ] || { echo "HATA: kanıt dosyasının sahibi uygulama kullanıcısı değil" >&2; exit 1; }
echo "ok: kanıt dosyası geri geldi, sahibi uygulama kullanıcısı"

echo
echo "DUMAN TESTİ GEÇTİ: kurulum, giriş, yedek, güncelleme ve geri yükleme çalışıyor."
