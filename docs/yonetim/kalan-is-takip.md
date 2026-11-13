# Kalan iş takibi

Kaynak: `ekran-kullanimi-kalan-is-sofor-maliyeti-v1.0.pdf`, Bölüm 8 (numaralar PDF'teki listeye göredir).
Güncelleme: 04.11.2026. Sonraki geliştirmeler bu listeden sürer; biten madde işaretlenir, silinmez.

| # | Madde | Öncelik | Tahmin (gün) | Durum |
|---|-------|---------|--------------|-------|
| 1 | Denetim kaydı okuma ekranı (8.1) | P1 | 3-4 | ✅ Yapıldı: `/panel/denetim`, menü, filtre, zincir doğrulama durumu |
| 2 | Bildirimler: kırmızı uyuşmazlık, süresi geçen, eksik belge (9.4) | P2 | 8-10 | ⏳ Bekliyor (kanal kararı gerekir) |
| 3 | QR/barkod kamerayla okutma (9.2) | P2 | 3-5 | ⏳ Bekliyor |
| 4 | Süresi geçen uyuşmazlık filtresi (7.6) | P2 | 1-2 | ✅ Yapıldı: liste filtresi + `due=OVERDUE` sorgu parametresi |
| 5 | Zayıf bağlantıda fotoğraf kuyruğu (9.3) | P2 | 4-5 | ⏳ Bekliyor (şoför kararına bağlı) |
| 6 | Şahit numune kaydı (2.5) | P2 | 3-4 | ⏳ Bekliyor |
| 7 | Kanıt ayrıntıları: galeri uyarısı, cihaz bilgisi, filigran | P2 | 4-5 | ⏳ Bekliyor |
| 8 | Eksik belge/foto, galeri oranı, düzeltilen kayıt raporu (8.5) | P2 | 3-4 | ⏳ Bekliyor |
| 9 | KVKK aydınlatma/onay + e-İrsaliye no alanı (9.5, 9.6) | P2 | 2-3 | ⏳ Bekliyor |
| 10 | Üretim Keycloak'ında OTP akışı (1.7) | P2 | 1-2 | 🟡 Kısmen: rehber `docs/yonetim/uretim-keycloak-otp.md`; konsolda uygulanıp doğrulanması kalıyor (kod değişikliği yok) |
| 11 | Sevkiyat geçmişinde şoför/taşeron isimleri çözülmüyor (H-24) | — | 1-2 | ✅ Yapıldı: geçmiş yanıtına `changedByName` (Keycloak hesaplarından çözülür), ekran adı gösterir; çözülemezse "Başka bir kullanıcı" |
| 12 | Seed sevkiyatlar olay (outbox/geçmiş) üretmiyor | — | 1-2 | ✅ Yapıldı: yerel seed her geçmiş adım için outbox olayı yazar (yayımlanmış sayılır; teslim alındı olayı hariç) |
| 13 | Faz 2/3: nem toleransı, konum, risk skoru, entegrasyonlar | P3 | 15+ | ⏳ Kapsam dışı |

Önerilen sonraki sıra: 2 ve 3. Şoför rolü kararı (PDF Bölüm 9) netleşmeden 5 bekletilir.
