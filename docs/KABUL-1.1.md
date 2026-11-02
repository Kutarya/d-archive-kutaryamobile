# Kutarya Mobile 1.1.0 — kabul kaydı

10 Ekim 2026. Yerel sürüm; kaynak ve çıktı D:/KutaryaMobile altında.

## Paket

- Android package com.kutarya.mobile; versionName 1.1.0, versionCode 2.
- min26 / target36 / compile37; önceki veritabanı ve imza korunur.
- Dagitim/Kutarya-Mobile-1.1.0.apk, 13.085.316 bayt.
- SHA256: 089136858EE2327D18CC086908BA6B8F446E0C3275CFC38AA7045DB87AE6137E.
- APK Signature Scheme v2 doğrulandı. Sertifika SHA256:
  a523304b2ebc9ef1a5b634b8d91b846c077bb60285335bc923ab94cc23960606.
- 1.0 APK ve kaynak ZIP korunur. Yeni kaynak arşivi yalnız allowlist dosyaları
  içerir; .secrets/.tools/build/veri/kanıt dizinleri arşive girmez.

## Değişiklikler

28 kayıtlı araç. Yerel dil modeli indirilmedi veya eklenmedi. Not arama ve
düzenleme, hatırlatma listesi/iptali, işaretlenebilir listeler, fener, medya
sesi ve Android ayar ekranları eklendi. Türkçe sayı sözcükleriyle matematik,
birim/yüzde/sıcaklık/tarih hesabı cihaz algoritmalarıyla çalışır.

Eksik saat için sonraki mesaj, son yanıtı not alma, doğal çok adımlı komutlar
ve rutin adıyla çalıştırma API istemez. Anlaşılmayan adımda plan başlatılmaz.
Not/mesajdaki "ve" sözcüğü yeni komut olarak yürütülmez. UI kutuları yapılandırılmış
araç çağrısı kullanır; madde/not metni yeniden komut olarak ayrıştırılmaz.

UTF-8 TXT/Markdown en fazla 200 KB: kaynak cümleleri seçerek özet, kelime/karakter
sayımı, dosyada satır arama. Metin içindeki talimatlar çalıştırılmaz. Özet,
üretken modelin belgeyi yorumlaması gibi sunulmaz. PDF ve görsel için mevcut
isteğe bağlı API yolu korunur; gerçek sağlayıcı kabulü yapılmadı.

Ana sayfadan API durumu/kurulum çağrıları kaldırıldı. Açık/koyu tasarım, dört
hızlı iş, komut araması, aranabilir not/geçmiş, plan ve liste ekranları var.
İşlem adımları ayrıntıya taşındı; gerekli arama/silme/düzenleme onayları korundu.
AI ayarı isteğe bağlı Ayarlar'dadır. JSON yedeğine liste ve hatırlatma eklendi;
API anahtarı ve görev geçmişi eklenmedi.

## Kontroller

- 49 JVM testi, 0 failure/error. OfflineSkillsTest 14; önceki dört takım
  toplam 35. Saat corpus'u 216, birim corpus'u 50 örnek içerir; bunlar test
  sayısına ayrıca eklenmez.
- Debug + release build ve lint başarılı: Kanitlar/v1.1-delivery-build.log.
  0 error / 19 warning; sürüm/target, KTX ve widget önerileri gizlenmedi.
- İlk cihaz koşusu 21/21 geçti: Kanitlar/v1.1-device-tests.log.
- Sonraki uçak modu koşusunda 20/21 geçti; eski Compose v1 test API'sinde
  performMeasureAndLayout called during measure layout hatası çıktı.
  Test ortamı v2/StandardTestDispatcher'a taşındı; testler kaldırılmadı.
- Düzeltilmiş test ortamıyla uçak modunda, Wi-Fi kapalı ve API anahtarı
  olmadan 21/21 geçti; süre 62.072 saniye. Kanıtlar:
  Kanitlar/v1.1-device-tests-v2-offline.log ve v1.1-offline-network.txt.
  Test APK'sı release paketine dahil değildir; gizli API politikası değişmedi.
- Ekran görüntüleri Kanitlar/v1.1-ui-light.png, v1.1-ui-dark.png,
  v1.1-records.png. İlk görünümde dört hızlı kartın tamamı görünür.

## Aynı imzalı release ile kabul

Test uygulamaları kaldırıldıktan sonra gerçek 1.0 release kuruldu. Gönder
düğmesiyle bir not kaydedildi; ardından 1.1 release adb install -r ile
güncellendi. Yeni sürüm aynı şifreli notu okuyabildi. İmza anahtarı ve
veritabanı korunur: Kanitlar/v1.1-upgrade-result.txt ve upgrade XML kayıtları.
Kurulu paket versionCode2 / versionName1.1.0; DEBUGGABLE bayrağı yok.

Gerçek Android dosya seçicisinden UTF-8, 250 baytlık test dosyası açıldı.
Uçak modu ve kapalı Wi-Fi korunurken Gönder ile kaynak cümlelerden özet
üretildi; API isteği veya dosyadaki metnin komut olarak yürütülmesi yok.
Kanitlar/v1.1-saf-summary-result.txt, v1.1-saf-summary.xml ve
v1.1-release-summary.png gerçek release akışının kanıtlarıdır.

Medya sesi yüzde 40 komutu AudioManager üzerinden uygulanıp okundu;
Kanitlar/v1.1-volume-result.txt. Wi-Fi ayarlarını aç komutu Android'in
gerçek WifiSettingsActivity ekranını açtı; intent monitor kullanılmadı:
Kanitlar/v1.1-native-settings-result.txt. ASSISTANT rolü verildikten sonra
KEYCODE_ASSIST uygulamayı açtı: Kanitlar/v1.1-assistant-role.txt.
Mevcut release işleminde main log buffer'da FATAL EXCEPTION yoktu;
Kanitlar/v1.1-release-health.txt. Bunlar özel emülatördeki sınırlı kabul
akışlarıdır; fiziksel telefon kabulü sayılmaz.

## Açık cihaz kabulü

Ortam özel Kutarya_Mobile_Test, Android API37/x86_64/16KB, RAM3072MB.
Gerçek ARM64 telefon, Türkçe mikrofon/TTS, gerçek rehber/hat/mesaj/takvim,
donanım feneri, widget, üçüncü uygulama bildirimleri ve Doze/yeniden başlama
teslim süresi ölçülmedi. Hatırlatma alıcısı test yayınıyla tetiklenir.
Alarm/arama/harita testlerinde native intent monitor'ü işlemi yakalar;
gerçek arama veya mesaj gönderimi kabulü değildir.

Sürekli uyandırma sözcüğü, genel ekran kontrolü ve yerel LLM yoktur. Genel
sohbet ve görsel yorumlama isteğe bağlı bulut bağlantısına bağlıdır.

## Teknik dayanaklar

[Compose v2 test geçişi](https://developer.android.com/develop/ui/compose/testing/migrate-v2),
[CameraManager](https://developer.android.com/reference/android/hardware/camera2/CameraManager),
[AudioManager](https://developer.android.com/reference/android/media/AudioManager),
[Android ayar intent'leri](https://developer.android.com/reference/android/provider/Settings).
