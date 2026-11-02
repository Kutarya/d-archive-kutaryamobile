# Kutarya Mobile 1.2.0 — kabul kaydı

10 Ekim 2026. Kaynak ve tüm araç/çıktılar D:/KutaryaMobile altında.
Ürün değerlendirmesi ve uygulanan plan GELISTIRME-1.2.md, kullanıcıya
yönelik özellik raporu RAPOR-1.2.md içindedir. Bu belge yerel ürün kabulüdür;
sonraki site yayını aşağıdaki ek kayıttadır.

## İmzalı paket

- Package com.kutarya.mobile; versionName 1.2.0, versionCode 3.
- min26 / target36 / compile37. Android 8+ paket tabanıdır.
- Dagitim/Kutarya-Mobile-1.2.0.apk: 13.134.468 bayt.
- SHA256: 371633E3CA4AA1E62EE36E5911EC5E5730FE49F237AB4ADD7B4A8245EC613500.
- APK Signature Scheme v2 doğrulandı; DEBUGGABLE bayrağı yok.
- Sertifika SHA256:
  a523304b2ebc9ef1a5b634b8d91b846c077bb60285335bc923ab94cc23960606.
- Eski 1.0/1.1 çıktıları korunur. Yayımlanmış 1.1 APK SHA256
  089136858EE2327D18CC086908BA6B8F446E0C3275CFC38AA7045DB87AE6137E;
  kaynak ZIP SHA256
  B0AF71DD13B36B1C0A5FE9A910B51E38F98EF231EF2EBBFB728CA9B1343E92C6.
  İki dosya bu görevde yeniden karşılaştırıldı; değişmedi.

## Son doğrulama

- **62 JVM testi:** 0 hata, 0 başarısız, 0 atlanan.
  Gradle XML sonuçları app/build/test-results/testDebugUnitTest altında.
- **32 Android testi:** 0 başarısız, 0 atlanan; son koşu 162,726 saniye.
  Kanitlar/v1.2-device-tests-passed.log: OK (32 tests).
  Test paketleri ayrı adb install/instrument komutlarıyla çalıştırıldı;
  ekran kanıtları paketler kaldırılmadan alındı.
- **Debug/release lint:** Her biri 0 error / 18 warning. Uyarılar
  gizlenmedi. XML raporları app/build/reports/lint-results-debug.xml ve
  lint-results-release.xml altında.
- **Son ürün ve test derlemesi:** JVM, debug APK, Android test APK,
  debug lint, release APK ve release lint tek koşuda başarılı.
  Kanitlar/v1.2-release-build-accepted.log: BUILD SUCCESSFUL in 1m 49s;
  138 görev, 25 yürütülen.
- **Ortam:** Kutarya_Mobile_Test, API37, x86_64, 16 KB, 3072 MB,
  1080x1920/density420, font scale1.0. Uçak modu1, Wi-Fi0 ve AI
  anahtarı olmadan. Test animasyon ölçekleri0; ürün hareketleri
  kaldırılmadı. Kanitlar/v1.2-environment-final.json.
- HTTP sağlayıcı sözleşmesi loopback MockWebServer ile sınandı.
  Gerçek ücretli model çağrısı veya canlı hesap kalite kabulü yapılmadı.

## Yeni testlerin doğruladıkları

- Eski görev JSON'u yeni alanlar olmadan okunur; eski kayıtlar korunur.
- Konuşma grupları ayrılır; yalnız seçili grubun son altı uygun tam
  soru/yanıt çifti, toplam 12.000 karakter sınırında bağlama girer.
  Özel içerik/anahtar filtresi, eski kayıtların otomatik aktarılmaması,
  önceki isteğin yeni telefon işlemine yetki vermemesi sınandı.
- Kibar Türkçe kalıplar, sesle yazılan bilinen uygulama adları ve
  not/mesaj gövdelerinin değişmemesi sınandı. İlgili öneri düğmesi
  yazı alanını doldurur; kendiliğinden araç çalıştırmaz.
- Günlük özetin gerçek hatırlatma ve açık liste kayıtlarını kullanması;
  yeni not/liste/hatırlatma formları; listeye ekleme, madde kaldırma,
  liste silme, onayı iptal etme ve onay sonrası gerçek kayıt sınandı.
- Geçersiz rutin kaydedilmez; desteklenen adımlar önceden görünür.
  Aynı adlı iki rutin arasından seçilen kartın kayıt kimliği çalışır.
- Önceki mesaj varken ses hizmeti durumunun görünmesi, sonucun
  gönderilmeden düzenlenebilmesi için varsayılan ayar ve mikrofon RMS
  değerlerinin sonlu/sınırlı olması doğrulandı. Mikrofona gerçek Türkçe
  konuşup tanıma kalitesi ölçülmedi.
- Açık/koyu ana ekran ve not formu gerçek native ekranlarda incelendi.
  Dört hızlı iş ilk görünümde bütünüyle görünür; klavye açık formda
  Kaydet/Vazgeç erişilebilir. Kanitlar/v1.2-home-light.png,
  v1.2-home-dark.png, v1.2-note-form.png, v1.2-records.png,
  v1.2-daily.png ve v1.2-conversation.png.
- Mevcut izin/onay, agent tur/adım sınırı, özel veri, iptal, native
  intent aktarımı ve yerel hesap/dosya testleri de son takıma dahildir.
  Instrumentation tarafından karşılanan intent, gerçek üçüncü
  uygulamada gönderim tamamlandı anlamına gelmez.

## Son imzalı release üzerinde gerçek akış

Özel test AVD'sine yayımlanmış 1.1.0/code2 release yeniden kuruldu.
Uygulama arayüzünden “Upgrade kabul f1.1-f1.2 f20261010” notu yazıldı ve
Notlar ekranında görüldü. Yukarıdaki SHA'ya sahip son 1.2.0/code3 APK
adb install -r ile üzerine kuruldu; aynı şifreli not yeni Notlar
ekranında okundu. Farklı imza veya veri temizleme kullanılmadı.

Kanıt: Kanitlar/v1.2-release-old-note.xml,
v1.2-release-upgrade-final-note.xml ve v1.2-release-upgrade-final.json.
Önceki ara 1.2 paketlerine ait upgrade kayıtları son paket kanıtı değildir.

Son release arayüzünden 17+28 ve 12+12 gönderildi; 45 ve 24 yanıtları
aynı konuşmada görüldü. am force-stop ile süreç kapatıldı ve am start ile
yeniden açıldı. İki istek ve iki yanıt geri geldi; son açık konuşma korundu.
Kanıt: Kanitlar/v1.2-release-chat-before-stop.xml,
v1.2-release-chat-after-restart.xml, v1.2-release-restart-final.json
ve v1.2-release-chat.png. Son release ana ekranı v1.2-release-home.png.
AVD crash buffer'da uygulama adına ait kayıt0; ölçüm
v1.2-release-runtime-final.json içindedir.

Bu güncelleme verisi test için üretilmiştir. Kullanıcının fiziksel
telefonu veya kişisel uygulama verisi okunmadı/temizlenmedi.

## Testte bulunan ve düzeltilen sorunlar

- Compose içinde StateFlow.value okunması lint hatası verdi; state
  collectAsState ile izlendi, formda busy ayrıca aktarıldı. Suppression yok.
- Kayıt eklenince eski liste kaydırması yeni kaydı gizliyordu. Bölüm/ilk
  kayıt değişiminde görünüm başa alınır; form kapanınca klavye kapatılır.
- Konuşma seçimi açık klavyede aşağıda kalıyordu. Sheet IME padding ve
  sınırlı ağırlıklı liste kullanır; satır açılabilir, aramada klavye
  kapatılabilir.
- Aynı adlı rutin kartları ada göre yanlış kayda gidebiliyordu.
  Kartlar doğrudan kayıt kimliğiyle çalıştırılır; test iki ayrı kayıtla sınar.
- Test notunun aynısı arama alanında da bulunduğunda iki semantik düğüm
  eşleşti. İçerik doğrulaması düzenlenebilir alanı dışlayacak şekilde
  daraltıldı; gerçek kayıt/persist assertion'ları korunur.
- API37 AVD'de yerel ses hizmeti bulunabildiği için test “hizmet yok”
  varsayımı yerine gerçek hizmet durumunu kontrol eder. Ses testi atlanmadı.

Önceki başarısız koşular Kanitlar altında korunur. Son kabul yukarıda
adı verilen 32 test koşusu ve son SHA'lı release üzerindedir.

## Kaynak arşivi ve veri sınırı

scripts/paketle.ps1 -Kaynak, dokuz kök dosya ile app/src, gradle,
scripts ve docs dosyalarını sürüm bazlı ZIP'e alır. .secrets/.tools/
build/Kanitlar/Dagitim, keystore/JKS, signing.properties, local.properties,
kullanıcı verisi ve model ağırlıkları dışarıda tutulur. Son arşiv her
entry için mevcut kaynak baytlarıyla SHA karşılaştırması ve özel dosya/
credential kontrolünden geçirilir. APK'da test paketi veya model yoktur.
ZIP SHA'sı kendisini içeren belgeye yazılmaz; döngü oluşturmaz.
Son APK/ZIP hash, entry ve audit sonucu
Kanitlar/v1.2-delivery-audit.json içinde tutulur; bu dosya ZIP'in dışındadır.

## Açık kabul sınırları

Fiziksel ARM64 telefon, gerçek Türkçe ASR/TTS ses kalitesi, hat/rehber,
mesaj/takvim uygulamalarının son işlemi, donanım feneri, widget,
üçüncü uygulamaların bildirim erişimi, Doze ve cihaz yeniden başlatmada
hatırlatma teslim süreleri bu koşuda doğrulanmadı. Android 8+ bütün
sürüm/markaları için fiziksel test yapıldığı iddia edilmez. Canlı Gemini
hesabının model erişimi, kota, ücret, gecikme ve cevap kalitesi ölçülmedi.

Yerel LLM/model dosyası eklenmedi. API'siz araçlar algoritmalar ve
Android hizmetleriyle çalışır. Genel üretken AI için kullanıcı bağlantısı
gerekir. Sürekli Hey Kutarya, bütün ekranları serbestçe yöneten agent
ve iOS sürümü yoktur. Yerel ürün kabulü sırasında site/mağaza yayını
yapılmadı; ortak servisler, Kutarya Cep ve diğer uygulamalar korundu.

Ses callback'leri ve isteğe bağlı bağlam biçimi için resmî dayanaklar:
[RecognitionListener](https://developer.android.com/reference/android/speech/RecognitionListener),
[SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer),
[Gemini generateContent](https://ai.google.dev/api/generate-content).
Bu belgeler cihazdaki Türkçe ses veya canlı model kalite ölçümünün yerine geçmez.

## Sonraki site yayını — 10 Ekim 2026

Kullanıcının “Siteye yayınla” talimatıyla son SHA'lı1.2 APK ve 177.608 bayt/
SHA71840129DDB6F626FC6B3FFC458B424BAF3F4629D7F24E4C2B8E866815E2CF57
kaynak ZIP'i değişmeden kutarya.com/proje/kutarya-mobile sayfasına eklendi.
Hosting ba161f8b5c0458d6; 1.1'in iki eski dosyası korundu. Site58/58 + build,
24 canlı HTML ve Edge1280/390 dört gerçek İndir düğmesinde tam byte/SHA
kabulü geçti. Ayrıntı D:/Kutarya/KutaryaWeb/docs/KUTARYA-MOBILE-1.2-YAYIN.md.

1.2 kaynak arşivi artık sabittir. Bu belge, README ve DEVIR sonraki yayın
kaydı için güncellendi; yayımlanmış ZIP yeniden oluşturulmaz. Arşivdeki
belgeler yerel teslim anını temsil eder. Play/fiziksel telefon/gerçek AI
kalite sınırları bu site kabulüyle kapanmış sayılmaz.
