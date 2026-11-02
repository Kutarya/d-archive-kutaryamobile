# Kutarya Mobile

Türkçe sesli Android asistanı. Uygulama adı **Kutarya Mobile**, asistan adı
**Kutarya Us**. Kaynak, Android araçları, imza anahtarı ve APK çıktıları
`D:\KutaryaMobile` altında tutulur. 1.2 sürümü
[kutarya.com](https://kutarya.com/proje/kutarya-mobile) üzerinde yayımlanmıştır;
eski1.1 dosyaları da korunur.

**1.2.0:** Devam edilebilir şifreli konuşmalar, günlük plan, doğrudan kayıt
formları ve doğrulanan rutin planları. 31 kayıtlı araç; yerel dil modeli
eklenmez. Not, liste, hatırlatma, hesap ve metin işleri API anahtarı istemez.
Özellik raporu docs/RAPOR-1.2.md; kabul kanıtları docs/KABUL-1.2.md.

## Kurulum ve ilk kullanım

1. `Dagitim\Kutarya-Mobile-1.2.0.apk` dosyasını Android telefona aktar ve kur.
   Android isterse APK'yı açtığın uygulamaya kurulum izni ver.
2. Asistan ekranında yazı yaz veya mikrofon düğmesine bas. Mikrofon ve rehber
   izinleri ilgili işlem istendiğinde sorulur. Sesle duyulan metin önce
   düzenlenebilir; Ayarlar'daki hemen gönder tercihi isteğe bağlıdır.
3. İsteğe bağlı genel AI için **Ayarlar → Ek AI bağlantısı** bölümüne kendi Gemini API
   anahtarını gir, model kimliğini seç ve metin aktarım tercihini aç.
   Anahtar [Google AI Studio](https://aistudio.google.com/apikey) üzerinden
   alınabilir; sağlayıcının kota ve ücretleri hesabına bağlıdır.
4. Üstteki konuşmalar simgesiyle geçmiş bir konuşmaya dön; + ile yeni konuşma
   başlat. Kayıtlar → Notlar/Planım/Listeler'deki + düğmesi doğrudan form açar.
5. Telefon komutları AI anahtarı olmadan da kullanılabilir. Telefon desteklerse
   Android ayarlarından varsayılan asistan seç; ana ekrana Kutarya widget'ı ekle.

API anahtarını sohbet alanına veya kaynak koda yazma. Anahtar Android Keystore
anahtarıyla şifrelenir. Bu dağıtımda ortak/önceden yüklenmiş bir API anahtarı yoktur.

## Android uyumluluğu

Kullanıcının standart sürüm tercihi doğrultusunda minimum Android 8 (API 26),
hedef SDK 36 esas alınır. Belirli bir telefon markasına özel yapılandırma yoktur.
İzinler ve asistan rolü cihazın Android sürümüne göre istenir.

Android 12 ve üzerinde cihazda yerel tanıma hizmeti varsa mikrofon bunu kullanır.
Diğer durumlarda kullanıcı çevrimiçi sesi Ayarlar'dan açabilir veya yazıyla
devam edebilir. Türkçe ses desteği cihazdaki hizmetlere bağlıdır. Yalnız API 37
emülatörüyle yapılan kabul, bütün Android sürümlerinin gerçek telefonda
denendiği anlamına gelmez; güncel kanıt kapsamı `docs\KABUL-1.2.md` içindedir.

## Yapabildikleri

| İstek | Davranış |
| --- | --- |
| `Saat kaç?`, `Telefon durumu` | Cihazın gerçek tarih, saat ve pil bilgisi |
| `Not al: yarın projeyi kontrol et` | Şifreli yerel not, kaydı yeniden okuyarak doğrulama |
| `Notlarımı göster` | Yerelde kayıtlı notlar |
| `Notlarımda toplantı ara` | Anahtar kelimelerin tümünü içeren notları bulma |
| `Not #15 güncelle: yeni içerik` | Onaydan sonra aynı notu düzenleme |
| `Son notumu sil` | Not içeriği gösterilip onaylandıktan sonra silme |
| `Bunu not al` | Son yanıtı içeriğiyle birlikte kaydetme |
| `Hesapla (17+28)*2` | Cihazda sınırlı matematik ayrıştırıcısı |
| `Yirmi beş çarpı dört` | Sayı sözcükleriyle matematik, gerçek işlem önceliği |
| `5 km kaç metre?`, `1200 lira yüzde 20 indirim` | Birim ve yüzde hesaplama |
| `20 gün sonra hangi gün?` | Cihaz takvimine göre tarih hesabı |
| `Yarın sabah yediye alarm kur` | Sistem Saat uygulamasına 07:00 alarm isteği; tarih ve kaydı orada kontrol et |
| `On dakika sayaç başlat` | Sistem sayacını açma |
| `20 dakika sonra su içmeyi hatırlat` | Yerel bildirim hatırlatması; pil koşulları nedeniyle gecikebilir |
| `Hatırlatmalarımı göster`, `Hatırlatma #12 iptal et` | Bekleyen kayıtları okuma; hedef onayıyla Android zamanlamasını iptal etme |
| `Günümü özetle`, `Bugün neler var?` | Gerçek yerel kayıtlardan bugünkü hatırlatmalar, geçmiş zaman uyarısı ve açık liste maddeleri |
| `Alışveriş listesi oluştur: süt, ekmek` | Kalıcı, işaretlenebilir liste |
| `Alışveriş listesine yumurta ekle` | Listeye madde ekleme |
| `Alışveriş listesinde süt tamamla` | Tamamlama; Kayıtlar → Listeler'de kutuyla da kullanılabilir |
| `Alışveriş listesinden süt kaldır`, `Liste #12 sil` | Tam hedefin içeriği gösterilerek onayla kaldırma |
| `Feneri aç`, `Medya sesini yüzde 40 ayarla` | Donanım varsa fener; Android'den geri okunan medya sesi |
| `WiFi ayarlarını aç` | İlgili Android ayar ekranına giriş |
| `Annemi ara` | Kişi/numara seçimi, hedef onayı; tercihe göre arama ekranı veya doğrudan arama isteği |
| `Ahmet'e on dakika gecikeceğimi yaz` | SMS taslağı; gönderimi kullanıcı tamamlar |
| `WhatsApp'ı aç` | Yüklü uygulamayı açma |
| `En yakın eczaneyi bul` | Haritada arama; konum iznini harita uygulaması yönetir |
| `İnternette ... araştır` | AI ayarı varsa kaynaklı araştırma; yoksa tarayıcıda arama sayfası |
| `Yarın 15:00 toplantı ekle` | Takvim taslağı; kullanıcı kaydeder |
| Desteklenen çok adımlı komut | `Saat kaç? ve Hesapla 17+28`; en fazla 8 adım, API gerekmez |
| Genel sohbet | İsteğe bağlı Gemini metin + kayıtlı araç çağrıları, en fazla 5 model turu ve 8 araç adımı |
| UTF-8 TXT/Markdown ekleme | En fazla 200 KB; yerelde cümle seçerek özet, kelime sayımı veya `Dosyada ... ara` |
| PDF ve resim ekleme | En fazla 6 MB; AI bağlantısı ve aktarım tercihiyle analiz |
| `Bildirimlerimi göster` | Tercih ve Android erişimi açıldıktan sonra alınan uygun bildirimler |

E-posta taslağı, WhatsApp mesaj taslağı ve kimliği belirtilen notun silinmesi
AI araçları arasında vardır. Silme ve gönderim taslağı hedefi ayrıca gösterilir.
Yerel kişi kısayollarıyla `annem` gibi adları bir numaraya bağlayabilirsin.

**Rutinler** sekmesinde kendi komutlarını ayrı satırlara yazarak kaydet ve
Çalıştır'a bas. Rutinler kendiliğinden çalışmaz. Görev geçmişinde her adım için
Doğrulandı, Uygulamaya aktarıldı veya Tamamlanamadı sonucu gösterilir.
Durdur düğmesi bekleyen onayı ve AI isteğini iptal eder; bitmiş dış işlem
geri alınmış sayılmaz.

Rutinini adıyla da çalıştırabilirsin: `Güne başla çalıştır`.
Eksik alarm saatinde sonraki `07:30` mesajı isteği tamamlar. Not ve mesaj
içeriğindeki `ve` sözcüğü yeni telefon komutu olarak çalıştırılmaz.
Desteklenmeyen adım varsa çok adımlı planın hiçbir adımı başlatılmaz.

## Sınırlar

- Sürekli `Hey Kutarya` dinlemesi, sınırsız ekran tıklama, banka işlemleri,
  satın alma, WhatsApp mesajını kendiliğinden gönderme ve iOS sürümü yoktur.
- Android'in resmî intent ve izin yolları kullanılır. Uygulamaya aktarım,
  dış uygulamada işlemin tamamlandığına dair kanıt değildir.
- Sistem Saat intent'i belirli bir takvim tarihi için standart bir parametre
  sunmaz. `yarın` isteğinde saat aktarılır; tarih kullanıcıya kontrol ettirilir.
- Yerel metin özeti üretken AI değildir; kaynak metinden cümle seçer.
  Dosya içindeki talimatları telefon komutu olarak çalıştırmaz.
- Genel AI ve kaynaklı araştırma internet ve geçerli Gemini erişimi ister.
  Anahtar yoksa başarılı AI bağlantısı varmış gibi gösterilmez.
- Yerel ses tanıma ve Türkçe seslendirme telefonun kurulu paketlerine bağlıdır.
  Çevrimiçi ses varsayılan kapalıdır; kullanıcı açmadan konuşma otomatik olarak
  çevrimiçi tanımaya gönderilmez.
- Bir not veya bildirim araca okunduktan sonra aynı görevde web araştırması
  engellenir. İçerik AI'ya yalnız ayrıca paylaşım tercihi açılmışsa iletilir.
- Asistan önceki konuşmaların tamamını otomatik olarak modele yüklemez.
  Açık konuşmadan en fazla altı uygun soru/yanıt ve 12.000 karakter bağlam
  kullanılır. Eski kayıtlar ve telefon işlemleri otomatik yüklenmez.
- Android 8+ minimum sürüm paket ayarıdır. Emülatör kabulü, gerçek ARM64 telefon
  ve gerçek ses/hat/AI sağlayıcısı kabulünün yerine geçmez. Kanıt kapsamı
  `docs\KABUL-1.2.md` dosyasında yazılır.

## Veri ve izinler

Not, görev, rutin ve bildirim içerikleri SQLite'a yazılmadan AES-GCM ile
şifrelenir; anahtar Android Keystore'dadır. API anahtarı ve ayarlar şifreli
özel depoda bulunur. Otomatik Android yedeği kapalıdır. Not ve rutin dışa
aktarımını kullanıcı başlatır; listeler ve hatırlatmalar da eklenir.
Her türün son 200 kaydı dışa aktarılır.
Bu JSON dosyası şifresizdir ve API anahtarını
ve görev geçmişini içermez.

Bildirim erişimi isteğe bağlıdır. Doğrulama/OTP içerikleri filtrelenir; bu
filtre her olası gizli içeriği tanıma garantisi değildir. En fazla 200 bildirim
7 gün tutulur; tercih kapanınca kayıtlar temizlenir. Dış paylaşımla gelen metin
yalnız yazı alanına yerleşir, kendi başına telefon komutu çalıştırmaz.

Rehber izni kişi adına aramada, telefon izni doğrudan aramada, bildirim izni
hatırlatmada gerekir. Tüm rehberin AI'ya aktarımı yapılmaz. Uygulama ayrıcalıklı
sistem uygulaması değildir ve root gerektirmez.

## Geliştirme

Native Kotlin + Jetpack Compose, Android Gradle Plugin 9.2.1, Gradle 9.4.1,
JBR 21; minimum SDK 26, hedef SDK 36, derleme SDK 37. Yerel kurulumda JBR,
Gradle, SDK, emülatör, AVD, cache ve geçici dosyalar `.tools` altındadır.

```powershell
& D:\KutaryaMobile\scripts\build.ps1 -Tasks ':app:assembleDebug',':app:testDebugUnitTest',':app:lintDebug'
& D:\KutaryaMobile\scripts\build.ps1 -Tasks ':app:assembleDebugAndroidTest'
& D:\KutaryaMobile\scripts\build.ps1 -Tasks ':app:assembleRelease',':app:lintRelease'
```

`.secrets\kutarya-mobile-release.jks` ve `.secrets\signing.properties`
güncellemeleri aynı kimlikle imzalamak için yerelde korunmalıdır. Bu dosyaları
paylaşma, kaynak arşivine koyma veya silme. Debug ve release imzaları ayrıdır;
telefona asıl dağıtım için `Dagitim` içindeki release APK'yı kur.

Ana bileşenler: `LocalParser`, `OfflineParser`, `OfflineUtilities`,
`LocalTools`, `ToolRegistry`, `AgentRunner`, `ToolExecutor`,
`GeminiClient`, `LocalStore`, `SpeechEngine`, `MobileModel`, `MobileUi`.
HTTP sözleşme testleri loopback MockWebServer kullanır; bu testler gerçek
sağlayıcı hesabının model/kota/ücret erişimini doğrulamaz.

Resmî dayanaklar:
[Android intent'leri](https://developer.android.com/guide/components/intents-common),
[ses tanıma](https://developer.android.com/reference/android/speech/SpeechRecognizer),
[asistan rolü](https://developer.android.com/reference/android/app/role/RoleManager),
[Gemini araç çağrıları](https://ai.google.dev/gemini-api/docs/function-calling),
[varsayılan model](https://ai.google.dev/gemini-api/docs/models/gemini-3.1-flash-lite).

Ürün planı `docs\PLAN.md`, devam bilgisi `DEVIR.md`, ölçüm ve loglar
`Kanitlar` altındadır.

10 Ekim1.2 site kabulü:
D:/Kutarya/KutaryaWeb/docs/KUTARYA-MOBILE-1.2-YAYIN.md.
1.1 ve1.2 kaynak ZIP'leri yayımlanmış sabit paketlerdir; bu sonraki yayın
kaydı için arşivler yeniden oluşturulmaz. ZIP'in içindeki belgeler yerel
ürün teslimi anındaki kayıtlardır.
