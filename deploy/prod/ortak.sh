#!/usr/bin/env bash
# deploy/prod betiklerinin ortak parçaları. Doğrudan çalıştırılmaz; betikler `source` ile alır.
# Bu dosyayı alan betik önce kendi klasörüne (deploy/prod) geçmiş olmalıdır.

ENV_FILE=.env
# Sunucuya özel ek ayar (ör. farklı port, bellek sınırı) docker-compose.override.yml dosyasına yazılır; git'e girmez.
# shellcheck disable=SC2034  # COMPOSE bu dosyayı alan betiklerde kullanılır
COMPOSE=(docker compose -f docker-compose.yml)
[ ! -f docker-compose.override.yml ] || COMPOSE+=(-f docker-compose.override.yml)
COMPOSE+=(--env-file "$ENV_FILE")

dur() {
  echo "HATA: $*" >&2
  exit 1
}

# .env içindeki bir değeri okur (yoksa boş döner).
env_al() {
  [ -f "$ENV_FILE" ] || return 0
  grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2- || true
}

# Ortam değişkeni verilmişse onu, yoksa .env içindeki değeri, o da yoksa varsayılanı döner.
ayar() {
  local ad="$1" varsayilan="${2:-}" deger
  deger="${!ad:-}"
  [ -n "$deger" ] || deger="$(env_al "$ad")"
  echo "${deger:-$varsayilan}"
}

# Bir servisin konteyner kimliği (çalışmıyorsa da döner; hiç oluşturulmamışsa boştur).
konteyner() {
  "${COMPOSE[@]}" ps -a -q "$1" 2>/dev/null | head -1
}

# Uygulama sağlıklı olana kadar bekler: $1 saniye (varsayılan 300). Olmazsa günlüğün sonunu gösterip durur.
uygulamayi_bekle() {
  local sure="${1:-300}" gecen=0 kimlik durum
  echo ">> Uygulamanın açılması bekleniyor (en çok ${sure} sn)..."
  while [ "$gecen" -lt "$sure" ]; do
    kimlik="$(konteyner yarnshipment-app)"
    if [ -n "$kimlik" ]; then
      durum="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$kimlik" 2>/dev/null || true)"
      [ "$durum" = healthy ] && return 0
    fi
    sleep 5
    gecen=$((gecen + 5))
  done
  echo "Uygulama ${sure} sn içinde hazır olmadı. Günlüğün sonu:" >&2
  "${COMPOSE[@]}" logs --tail 60 yarnshipment-app >&2 || true
  return 1
}

# Kanıt dosyalarının sunucu diskindeki klasörü (evidence-data birimi).
kanit_klasoru() {
  local kimlik yol=""
  kimlik="$(konteyner yarnshipment-app)"
  if [ -n "$kimlik" ]; then
    yol="$(docker inspect -f '{{range .Mounts}}{{if eq .Destination "/data/evidence"}}{{.Source}}{{end}}{{end}}' "$kimlik" 2>/dev/null || true)"
  fi
  if [ -z "$yol" ]; then
    yol="$(docker volume inspect -f '{{ .Mountpoint }}' yarn-shipment-prod_evidence-data 2>/dev/null || true)"
  fi
  [ -n "$yol" ] || dur "kanıt klasörü (evidence-data birimi) bulunamadı; kurulum yapıldı mı?"
  echo "$yol"
}
