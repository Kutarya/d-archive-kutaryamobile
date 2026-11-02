# Kutarya Mobile — devir kaydı

9 Ekim 2026. Kullanıcı önceki planın uygulanmasını, D sürücüsünde ayrı
klasör ve sonunda APK derlemesini istedi. Kök D:/KutaryaMobile.

10 Ekim'de yerel Android 1.0 MVP ve imzalı APK teslimi tamamlandı.
Ardından kullanıcı yerel modeli kullanmadan API'siz kabiliyetlerin artmasını,
tasarımın iyileştirilmesini, gereksiz metinlerin kaldırılmasını ve yeni APK istedi.
Sonraki “Siteye yayınla” talimatıyla yayımlanmış sürüm 1.2.0/code3.
Plan docs/GELISTIRME-1.2.md, özellikler docs/RAPOR-1.2.md,
kabul docs/KABUL-1.2.md. Önceki 1.1 plan/kabul kayıtları korunur.
İlk plan docs/PLAN.md; önceki kabul docs/KABUL.md. Mevcut Kutarya Cep kaynağı
değiştirilmedi. İlk geliştirme aşamasında yayın yoktu; sonraki “Sitede yayınla”
talimatıyla 10 Ekim'de site/katalog/Storage yayını tamamlandı. Mağaza yayını yok.

Derleme JBR21/Gradle9.4.1 kopyaları ve SDK/build-tools ile .tools altında.
Gradle önbelleği ve geçici dosyalar bu kökte tutulur.

## Son durum

### 10 Ekim 2026 — 1.2 site yayını tamamlandı

- Kullanıcı yerel APK tesliminden sonra “Siteye yayınla” dedi.
  https://kutarya.com/proje/kutarya-mobile, güncel1.2.0.
  Aynı imzalı APK/kaynak ve dört gerçek emülatör ekranı mevcut
  kutarya-ai-3169 admin/Storage/katalog/Hosting hattıyla yayımlandı.
- Hosting ba161f8b5c0458d6; önceki f57f0560937b8525. Site58/58 + build,
  24 canlı HTML SHA/canonical/sitemap ve Edge1280/390 kabulü geçti.
  Gerçek İndir düğmeleriyle yeni iki ve eski iki dosya tam indirildi.
  1.2 APK SHA371633E3CA4AA1E62EE36E5911EC5E5730FE49F237AB4ADD7B4A8245EC613500;
  kaynak177.608 bayt/62 dosya/SHA
  71840129DDB6F626FC6B3FFC458B424BAF3F4629D7F24E4C2B8E866815E2CF57.
- 1.1'in iki dosyası korundu;1.2'nin iki dosyası Dosyalar'da başta.
  Artık1.1 ve1.2 APK/kaynak ZIP'leri yayımlanmış sabit dosyalardır.
  Bu devir/README/kabul belgesi yayın kaydı için güncellendi; ZIP'leri
  belge değişikliği için tekrar paketleme veya üzerine yazma.
- Diğer11 proje, sekiz servis, kurallar/Auth/admin claim'leri ve IAM aynı.
  Geçici imza izni yüklemeden önce kaldırıldı; port/parola/key payload yok.
  PC bağımlılığı yok. StorageUS-EAST1/gamesService europe-west1;
  trafik/depolama mevcut Firebase planındadır. Play yayını yapılmadı.
- Kanıt D:/Kutarya/KutaryaWeb/artifacts/releases/kutarya-mobile/1.2.0/
  ve artifacts/kutarya-mobile/1.2.0/. Yayın kabulü
  D:/Kutarya/KutaryaWeb/docs/KUTARYA-MOBILE-1.2-YAYIN.md.
  Android ürün62+32 kabulü, fiziksel ses/telefon/AI açık sınırları korunur.
  Aşağıdaki yerel geliştirme kabulü bu site yayınından önceki aşamadır.

### 10 Ekim 2026 — 1.2 uygulama geliştirmesi ve yerel teslim

- Kullanıcı önce değerlendirme, ardından plan/uygulama ve madde madde
  uygulama raporu istedi. D:/KutaryaMobile yerinde geliştirildi.
  31 araç; yerel LLM eklenmedi. 1.2 yeni site yayını yapılmadı.
- Devam eden şifreli konuşmalar, seçme/arama/onaylı silme, son açık konuşmanın
  geri gelmesi. Eski görev JSON'u uyumlu; boş kimlik Eski kayıtlar grubudur.
  AI bağlamı yalnız aynı grubun son6 uygun tam çift/12000 karakteri;
  özel veri paylaşım kapısı ve yeni isteğe bağlı araç yetkisi korunur.
- Kompakt açık/koyu ana ekran, gerçek günlük plan sayıları/özeti, hızlı
  kayıt formları, mesajlar ve doğrulanmış adımlar; kopyala/not al/yeniden kullan.
  Türkçe kibar kalıp/ses adı, ilgili öneri ve metin gövdesini koruma.
- Ses kısmi metin/RMS, varsayılan düzenle-sonra-gönder, isteğe bağlı hemen
  gönder; hizmet durumunun eski mesaj varken de görünmesi. Gerçek Türkçe
  ses kalitesi kabulü değildir. Çevrimiçi ASR/TTS tercihi ayrı korunur.
- Not/liste/hatırlatma formları, madde ekleme/onaylı kaldırma, listeyi
  onaylı silme; geçersiz rutin planı kaydedilmez. Aynı adlı kartlar
  runRoutine(id) ile seçilen kayıt kimliğini çalıştırır.
  Liste yazma sonuçları da privateData=true: AI/veri/web kapısı atlanmaz.
- İmzalı Dagitim/Kutarya-Mobile-1.2.0.apk, 13.134.468 bayt; code3,
  release/v2, DEBUGGABLE yok, aynı sertifika. SHA256:
  371633E3CA4AA1E62EE36E5911EC5E5730FE49F237AB4ADD7B4A8245EC613500.
  Yayımlanmış1.1 APK ve kaynak hash'leri yeniden ölçüldü; değişmedi.
- 62 JVM + 32 Android testi geçti; atlanan0. Son32 koşu162,726s,
  Kanitlar/v1.2-device-tests-passed.log. Uçak1/Wi-Fi0/anahtarsız API37
  x86_64/16KB AVD. Debug/release lint0 error/18 warning; suppression yok.
  Son derleme Kanitlar/v1.2-release-build-accepted.log, 1m49s başarılı.
- Son hash'li1.2, gerçek1.1 release üzerine install -r ile kuruldu;
  eski şifreli not gerçek Notlar ekranında görüldü. Son release'te
  iki hesap mesajı force-stop/start sonrası geri geldi. Kanitlar/
  v1.2-release-upgrade-final.json ve v1.2-release-restart-final.json.
- Fiziksel telefon/ASR-TTS/hat/üçüncü uygulama/bildirim/widget/Doze ve
  canlı sağlayıcı kalite kabulü açık. Sınırsız ekran agent'i/hotword/iOS yok.
  JSON yedeği her türün son200 kaydı; sohbet/anahtar içermez, import yok.
  Kaynak arşivi allowlist; audit Kanitlar/v1.2-delivery-audit.json'da.

### Önceki 1.1 yayını ve tabanı

- Canlı1.1: https://kutarya.com/proje/kutarya-mobile. Hosting f57f0560937b8525;
  APK ve kaynak ZIP gerçek İndir düğmeleriyle byte/SHA doğrulandı. Kaynak
  146.803 bayt/SHA256 B0AF71DD13B36B1C0A5FE9A910B51E38F98EF231EF2EBBFB728CA9B1343E92C6.
  Site58/58 + build,24 HTML ve Edge1280/390 kabulü geçti. Diğer11 proje/
  sekiz servis/kurallar/Auth/claim'ler korundu; geçici IAM kaldırıldı.
  Ayrıntı D:/Kutarya/KutaryaWeb/docs/KUTARYA-MOBILE-YAYIN.md ve
  artifacts/release-2026-10-10-kutarya-mobile/. 1.1 kaynak ZIP'i yayımlanmış
  sabit pakettir; devir belgesi için yeniden paketleyip üzerine yazma.

- 10 Ekim kullanıcı telefon sorusuna "default sürümlere göre yap" yanıtı verdi.
  Genel Android 8+ tabanı korunur; modele/markaya özel profil seçilmedi.
  min26/target36/compile37 ve sürüm denetimli izin/ses/asistan yolları
  kaynakta doğrulandı. API31+ yerel ses hizmeti bulunursa kullanılır; diğer
  cihazlarda açık çevrimiçi ses tercihi veya yazı gerekir. Uyumluluk README'ye
  eklendi. Kaynak ZIP güncellendi; APK kodu/imzası/sürümü değişmedi.
- Kotlin/Compose, package com.kutarya.mobile, min26/target36/compile37.
  Kayıtlı 28 telefon/AI aracı; yerel Türkçe ayrıştırıcı, şifreli not/geçmiş/
  rutin/bildirim, Keystore API anahtarı, ses/TTS, belge eki, asistan rolü
  ve widget kaynağı. Kullanıcıya görünen asistan Kutarya Us.
- Genel AI Gemini REST + function calling; sabit resmî HTTPS hedefi.
  Ortak anahtar yok. Kendi anahtarı ve aktarım tercihi Ayarlar'dan girilir.
  Model önerisi şema/kullanıcı isteği doğrulamasından geçer. En fazla
  5 model turu / 8 adım; tekrar yazma, iptal ve kişisel veri sınırı var.
- API'siz: not arama/düzenleme, hatırlatma listeleme/iptal, yapılacak listeleri,
  fener, medya sesi, ayar ekranları, Türkçe sözlü sayılarla matematik,
  birim/yüzde/sıcaklık/tarih hesapları ve bağlam tamamlayan komutlar eklendi.
  Yerel LLM/model dosyası eklenmedi. UTF-8 TXT/Markdown 200 KB sınırında
  kaynak cümlelerinden özet, metin sayımı ve satır arama yapılır.
  Dosya ve not metni komut diye yürütülmez; UI yapılandırılmış çağrı kullanır.
- Açık/koyu UI sadeleşti; dört hızlı kart, aranabilir komutlar/notlar/geçmiş,
  plan/listeler, rutin taslakları ve son yanıtı not alma var. API kurulum
  metinleri ana sayfadan kaldırıldı; gerekli işlem onayları korunur.
- Dagitim/Kutarya-Mobile-1.1.0.apk: release, 13.085.316 bayt, v2 imza,
  versionCode2. SHA256 089136858EE2327D18CC086908BA6B8F446E0C3275CFC38AA7045DB87AE6137E.
  Sertifika SHA256 a523304b2ebc9ef1a5b634b8d91b846c077bb60285335bc923ab94cc23960606.
  1.0 APK/kaynak korunur; sürüm bazlı APK-KAYDI JSON ve kaynak ZIP üretilir.
- 49 JVM test + 21 Android emülatör testi geçti. Son 21 test uçak modu/
  Wi-Fi kapalı/anahtarsız çalıştı: Kanitlar/v1.1-device-tests-v2-offline.log.
  Lint debug/release 0 error / 19 warning. Son ürün derlemesi
  Kanitlar/v1.1-delivery-build.log; test ortamı derlemesi v1.1-test-harness-build.log.
- Gerçek 1.0 release'e not yazıldı, 1.1 aynı imzayla üzerine kuruldu ve
  eski şifreli not okundu. Gerçek Android dosya seçicisiyle TXT özeti,
  medya sesi %40, gerçek Wi-Fi ayar ekranı ve ASSIST tuşu doğrulandı.
  Bu release akışları da uçak modunda yapıldı. Kanıtlar KABUL-1.1.md'de.
  APK'ya test verisi/API anahtarı gömülmedi; release DEBUGGABLE değildir.
- .secrets/kutarya-mobile-release.jks ve signing.properties kalıcı güncelleme
  imzasıdır: koru, paylaşma, kaynak ZIP'e alma. scripts/paketle.ps1 -Kaynak
  yalnız yerel dağıtım dosyası/allowlist kaynak arşivi üretir.
- Gerçek ARM64 telefon, Türkçe mikrofon/TTS, hat/rehber/mesaj uygulamaları,
  widget, gerçek bildirim erişimi, Doze/yeniden başlatmada hatırlatma ve canlı
  Gemini anahtarı ve donanım feneri kabulü açık. Tam sınırlar docs/KABUL-1.1.md. Hey Kutarya,
  sınırsız ekran kontrolü, AppFunctions ve iOS geliştirilmiş sayılmaz.

## Derleme ve test tuzakları

- Java PATH'teki26 yerine .tools/jbr içindeki JBR21 kullan. Gradle9.4.1 ve özel D cache;
  tek worker havuzu2, Xmx1536m/metaspace768m. SDK37.0 ve build-tools36 kullanılır.
- Elle açılan SDK'da package.xml olmadığında AGP android-37.0-2 oluşturur;
  çift SDK dizini bırakma. Doğru SDK yöneticisi paketini canonical yola al.
- Özel Kutarya_Mobile_Test API37/x86_64/16KB Play AVD'si3072MB. İlk boot
  2048MB'de LOW_MEMORY çıkışı verdi; bu uygulama crash'i diye yorumlanmadı.
- Compose test transitive Espresso3.5 bozuktur; androidTest'te3.7 açık tutulur.
  Gizli API politikası değiştirilmedi. Test APK'sı release dağıtımına girmez.
- Compose v1 test kuralının Unconfined dispatcher'ı API37/Compose1.12 ortamında
  performMeasureAndLayout called during measure layout hatası verdi.
  androidx.compose.ui.test.junit4.v2.createAndroidComposeRule kullanıldı;
  StandardTestDispatcher ile aynı 21 test çevrimdışı geçti. Test silme veya
  Android'in gizli API politikasını gevşetme çözüm değildir.
- Arama/mesajlar hedef onayıyla; native aktarım HANDOFF. Intent açmak dış
  işlemi bitirmez. geo: URI opaque olduğu için Uri.encode ile oluşturulur.
- Kibar/noktalı komutta metin argümanını normalize edilmiş offset'ten alma.
  Not içindeki ‘nasıl’ soru kelimesi komut niyetini değiştirmez.
- Compose'ta StateFlow.value ile UI okuma lint hatasıdır; collectAsState veya
  üstten alınan state kullan. Sheet'te arama klavyesi için IME padding ve
  ağırlıklı/sınırlı liste gerekir; seçilecek satırı klavyenin altında bırakma.
- Yeni kaydı görünür yapmak için kayıt/rutin listesini bölüm/ilk kayıt ID'si
  değişince başa al. Aynı adlı rutini karttan ada göre değil kayıt ID'siyle aç.
- Android assertion'ında aynı metin arama alanında da olabilir:
  içerik düğümünü !hasSetTextAction() ile ayır; gerçek kayıt kontrolünü kaldırma.


## 2 Kasım 2026 — GitHub arşiv aktarımı

Kullanıcı D: projelerini GitHub'a aktarıp ardından yerel kopyaları silmeyi istedi.
Hedef özel depo: https://github.com/Kutarya/d-archive-kutaryamobile. Kaynak anlık kopyası ve
şifreli tam dosya arşivleri aktarılır; sırlar kaynak görünümünden çıkarılır.
Yerel silme yalnız bütün dosyaların arşiv CRC/boyut denetimi, GitHub SHA-256
eşleşmesi ve son kaynak değişiklik kontrolünden sonra yapılabilir. Bu not
tek başına aktarımın veya silmenin tamamlandığını kanıtlamaz; nihai durum
GitHub release dosyaları ve sahibine teslim edilen aktarım raporundadır.
