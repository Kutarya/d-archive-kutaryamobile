# Kutarya Mobile — APK ve kabul kaydı

Güncel 1.1.0 kabulü [KABUL-1.1.md](KABUL-1.1.md) dosyasında.
Aşağıdaki kayıt 1.0.0 sürümünün korunmuş kabulüdür.

10 Ekim 2026. Bu kayıt yerel Android MVP teslimidir. Mağaza yayını,
gerçek telefon kabulü veya canlı AI hesabı kabulü değildir.

## Teslim

- Kök: `D:\KutaryaMobile`
- İmzalı release APK: `Dagitim\Kutarya-Mobile-1.0.0.apk`
- Paket: `com.kutarya.mobile`, sürüm `1.0.0`, versionCode `1`
- Minimum Android API `26` (Android 8), hedef API `36`, derleme API `37`
- Boyut: **13.019.712 bayt**, yaklaşık **12,42 MiB / 13,02 MB**
- APK SHA-256: `8F23EAB07B006A11D71EC9775D19CC81AE1758227AFDCBBEBF5309AC883A7D67`
- APK Signature Scheme **v2 doğrulandı**. Release sertifikası SHA-256:
  `a523304b2ebc9ef1a5b634b8d91b846c077bb60285335bc923ab94cc23960606`.
- JBR21/Gradle9.4.1, SDK, emülatör/AVD, cache/geçici dosyalar `.tools` altında.
- İmza anahtarı `.secrets` altında yerelde korunur; kaynak arşivine girmez.

1.0 paket bilgisi `Dagitim\APK-KAYDI-1.0.0.json`. Genel `APK-IMZA.txt`, Android
paket çıktısı `APK-BILGI.txt` ve `SHA256.txt` son paketlenen sürüme aittir.

## Geçen kontroller

**35 JVM testi**, sıfır hata/başarısız test:

| Takım | Test |
| --- | ---: |
| LocalParserTest | 12 |
| ToolSafetyTest | 6 |
| AgentRunnerTest | 9 |
| GeminiClientTest | 8 |

Yerel ayrıştırıcı testi ayrıca 216 saat/dakika/ifade kombinasyonunu tek corpus
testinde dolaşır. Türkçe harfler, apostrof, `yediye`, kibar/sonu noktalı komut,
not içindeki soru/boşluk, belirsiz saat, süre ve rutin sınırı kontrol edildi.
`araştır` arama yetkisi vermez; bilgi sorusu telefon işlemi başlatmaz.
Araç şeması/tür/süre sınırı, bilinmeyen araç, kullanıcı niyeti, tekrar yazma,
adım/tur sınırı, iptal, kişisel verinin saklanması ve modelin işlemi yapılmış
gibi söylemesi kontrol edildi.

Gemini testleri **loopback MockWebServer sözleşme testidir**: anahtar URL'ye
girmez, yönlendirmede başka sunucuya taşınmaz, hata gövdesi kullanıcıya
yansıtılmaz, function id/thoughtSignature korunur, kaynaklar süzülür.
Gerçek sağlayıcıdan AI yanıtı alınmış sayılmaz.

**13 Android testi**, tamamı geçti; `device-tests-final.log` sonucu
`OK (13 tests)`:

- Yazı alanı/Gönder üzerinden not kaydı, not listesi, yeniden açılan veri
  bağlantısında okuma ve SQLite'ta içeriğin şifreli olması.
- API anahtarı olmadan gerçek cihaz tarih/pil bilgisi ve matematik sonucu.
- Ekrandan rutin oluşturma ve iki araç adımını çalıştırma.
- Arama onayı verilmeden intent çıkmaması; onayda tam bir intent.
- Onay iptali ve durdurmada sonradan arama intent'i çıkmaması.
- Dış paylaşımdaki metnin yazı alanına gelmesi ve kendiliğinden çalışmaması.
- Alarm/harita intent aktarımı ve olmayan uygulamanın başarısız bildirilmesi.
- Keystore ile anahtarın şifreli saklanması ve yeni depodan tekrar okunması.
- Hatırlatma planlama ve gerçek Android alıcısı/NotificationManager ile
  bildirim üretme. Alıcı **test yayınıyla** tetiklendi; beş dakikanın geçmesi
  veya Doze altında teslim süresi ölçülmedi.
- AI ayarının anahtar yokluğunu belirtmesi; açık/koyu tema ve ayar ekranı.

Arama, alarm ve harita testlerinde dış intent'ler Instrumentation monitor'ü
ile yakalanır; gerçek kişi aranmadı veya sistemde alarm kuruldu iddia edilmez.

**Debug ve release derlemeleri/lint tamamlandı.** Son derleme:
`verified-build.log`, `BUILD SUCCESSFUL in 1m 1s`. Lint'te sıfır error;
19 warning vardır (sürüm/target, KTX ve widget metin önerileri). Bunlar saklanır,
başarılı test diye gizlenmez. İlk geçerli APK'dan sonra bulunan sorunlar
düzeltilip ilgili takımlar yeniden çalıştırılmıştır.

## Doğrudan release APK denemesi

Test ortamı: projeye ait **Kutarya_Mobile_Test**, emülatör `emulator-5562`,
Android API37.0, x86_64, 16 KB page size sistem imajı, 3072 MB RAM,
1080×1920 / 420 dpi. Bu bir ARM64 fiziksel telefon değildir.

Debug/test paketleri yalnız bu emülatörden kaldırılıp **teslim edilecek aynı
imzalı release APK** kuruldu: adb `Success`. İlk açılış `Status: ok`,
`LaunchState: COLD`, `TotalTime: 1467 ms`. Bu tek emülatör ölçümüdür;
genel performans veya pil iddiası değildir.

Release uygulamasının yazı alanına `Ayarlar ac` verilip gerçek Gönder düğmesine
basıldı. Android'in üst etkinliği **com.android.settings/.Settings** oldu;
geri dönünce uygulama `Settings uygulamasını açtım.` ve `Uygulamaya aktarıldı`
gösterdi. Bu denemede intent monitor'ü yoktu.

Release APK, Android asistan rolüne atanabildi. Home → ASSIST tuşu sonrası
üst etkinlik `com.kutarya.mobile/.MainActivity` olarak doğrulandı.
AndroidRuntime hata kontrolünde bu release denemesinde fatal exception yok.

Kanıtlar: `release-first-launch.log`, `release-native-handoff.txt`,
`release-result.xml`, `release-assistant-role.txt`, `release-installed-package.txt`,
`release-crash-check.log`; `ui-light.png`, `ui-dark.png`, `release-home.png`.

## Açık kabul sınırları

- Gerçek telefonda Türkçe konuşmanın çözülmesi, Türkçe TTS sesi, gerçek
  rehber/hat araması, SMS/WhatsApp/e-posta/takvim uygulamaları ve widget
  yerleşimi denenmedi. Android izinleri ve uygulama desteği cihazda kontrol edilir.
- Gerçek Gemini anahtarı kullanılmadı; model listesi, sohbet, araç seçiminin
  gerçek model doğruluğu, PDF/görsel yanıtı, güncel kaynaklı araştırma ve kota
  kabulü açık. Kullanıcı kendi anahtarını Ayarlar → AI bağlantısına ekler.
- Bildirim dinleyicisinin gerçek üçüncü uygulama bildirimleriyle uçtan uca
  kabulü ve OTP filtresinin tüm içerik türleri için başarısı ölçülmedi.
- Hatırlatmanın gerçek zaman geçişi/Doze/yeniden başlama pil denemesi açık.
- Sürekli `Hey Kutarya`, serbest ekran agent'i ve iOS bu sürümde yoktur.
- Sistem Saat/takvim/mesaj aktarımı kullanıcıya bırakılan kaydı veya gönderimi
  tamamlanmış saymaz. Android alarm intent'inde belirli gün parametresi yoktur;
  `yarın` söylenince tarihi Saat uygulamasında kullanıcı kontrol eder.

## Tekrar yaşanmaması gereken tuzaklar

1. Elle SDK37 imajını açmak `package.xml` üretmez. AGP ikinci kopyayı
   `android-37.0-2` altına kurdu. SDK yöneticisinin ürettiği paket doğru
   `platforms/android-37.0` konumuna alındı; manuel kopya ayrı arşivlendi.
2. Yeni Android37 Play imajı ilk açılışta 2048 MB RAM'de uygulamayı
   LOW_MEMORY nedeniyle kapattı; Java/native crash değildi. Özel test AVD'si
   3072 MB yapıldı. Eski AVD veya gerçek telefon değiştirilmedi.
3. Compose UI testi transitif Espresso3.5.0 getirdi; API37'de gizli
   InputManager.getInstance erişimi bozuldu. Yalnız test bağımlılığına açık
   Espresso3.7.0 eklendi; Android gizli API politikası gevşetilmedi.
4. ActivityScenario, onNewIntent sonrasında değişen activity.intent ile
   kapanışı izleyemedi. Testte paylaşım denemesi sonrası orijinal intent
   geri verildi. LazyColumn içindeki tema için önce liste kaydırıldı.
5. Opaque `geo:` URI'ye appendQueryParameter kullanılamaz; query Uri.encode
   ile geo URI'sine kuruldu ve gerçek Android intent testi geçti.
6. Windows'ta emülatör ana işleminin bitmesi qemu alt işleminin hemen
   çıktığı anlamına gelmez. Aynı AVD yeniden açılmadan alt işlem ve kilit
   durumu kontrol edilir. Başka projelerin emülatörleri kapatılmaz.

Yerel yayın paketi hazırlama: `scripts\paketle.ps1 -Kaynak`. Bu komut
**Hosting/Storage/Play yayını yapmaz**. `.secrets`, `.tools`, build çıktıları,
canlı kayıtlar ve kimlik bilgileri kaynak ZIP'e alınmaz.
