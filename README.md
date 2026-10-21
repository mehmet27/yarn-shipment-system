# yarn-shipment-system

İplik sevkiyat, teslimat ve kalite kontrol sistemi.

## Dokümanlar

- [Java 25 kod standartları](docs/standartlar/java-kod-standartlari.md): projedeki tüm Java kodu için bağlayıcı kurallar.
- [React ve TypeScript kod standartları](docs/standartlar/react-typescript-kod-standartlari.md): frontend kodu için bağlayıcı kurallar.
- [Veritabanı tasarımı](docs/veritabani-tasarimi.md): servis başına PostgreSQL 17 veritabanları ve tablolar.

## Yerel veritabanı

```bash
cp deploy/.env.example deploy/.env      # şifreleri değiştirin; dosya git'e girmez
docker compose -f deploy/docker-compose.yml up -d
```

Compose, tek bir PostgreSQL 17 konteynerinde 8 servis veritabanını ve Keycloak veritabanını, her servis için
`<servis>_migrator`, `<servis>_app` ve `<servis>_readonly` rolleriyle açar. Tabloları servisler açılışta Flyway
ile kurar; migration'lar `services/<servis>/src/main/resources/db/migration` altındadır.

Migration'ları ve veritabanı kısıt testlerini servisler olmadan doğrulamak için:

```bash
tools/db/verify-migrations.sh
```
