# Kutarya Mobile — sesli mobil asistan ve agent geliştirme planı

10 Ekim 1.2 geliştirmesi: kalıcı konuşmalar, günlük plan, doğal Türkçe,
doğrudan kayıt formları, düzenlenebilir ses ve rutin doğrulama.
Güncel plan GELISTIRME-1.2.md, özellik raporu RAPOR-1.2.md ve kabul
KABUL-1.2.md'dedir. Aşağıdaki ilk ürün planı takip fazlarını da içerir.

10 Ekim 1.1 geliştirmesi: API ve yerel LLM gerekmeden komut kapsamı genişletildi,
UI sadeleştirildi. Güncel kapsam GELISTIRME-1.1.md, kabul KABUL-1.1.md'dedir.

9 Ekim 2026. Bu belge ilk ürün ve uygulama planını korur.
10 Ekim'de D:/KutaryaMobile altında yerel Android 1.0 MVP geliştirildi ve
imzalı APK alındı. Gerçekleştirilen kapsam ve açık cihaz/API kabulü KABUL.md
altındadır; bu plandaki bütün takip fazları yapılmış veya mağazada
yayımlanmış sayılmaz.

## 1. Ürün kararı

**Kutarya Mobile**, kullanıcının Türkçe konuştuğu, telefon işlerini yerine
getiren ve desteklenen hizmetlerde çok adımlı görev yürüten bir mobil asistan
olacak. Uygulamanın adı Kutarya Mobile, kullanıcıya görünen asistan adı
Kutarya Us olarak korunur.

İlk hedef Android. Ses, telefon işlemleri ve görev yürütme için native Android
uygulaması önerilir. iPhone sürümü, Android çekirdeği doğrulandıktan sonra
platformun sunduğu işlem kapsamıyla hazırlanır.

Ürünün vaadi desteklenen görevleri güvenilir biçimde tamamlamaktır. Bir
uygulamayı açmak, o uygulamanın içinde istenen işlemi tamamlamakla aynı değildir.
Her özellikte doğrudan işlem, kullanıcıya aktarım ve doğrulanabilen sonuç
ayrı tanımlanır.

## 2. Güncel platform kararları

| Alan | Planı etkileyen gerçek | Uygulama kararı |
| --- | --- | --- |
| Telefon işlemleri | Android'in ortak intent'leri arama, alarm, harita, takvim ve mesaj hazırlama gibi işlere giriş sağlar. | Önce resmî işlem yolları ve dönen sonuçlar kullanılır. |
| Genel ekran agent'i | Google Play, genel asistanların Accessibility API ile otonom planlama ve işlem yapmasını yasaklar. Statik, insan tanımlı otomasyon ayrı değerlendirilir. | Serbest ekran tıklama agent'i ilk ürün mimarisine alınmaz. |
| AppFunctions | Android 16+ üzerinde uygulamaların açtığı fonksiyonlar keşfedilebilir; çağıran uygulama izni ve çalışma zamanında izin listesi denetimi vardır. | Kutarya'nın çağırıcı erişimi ve hedef uygulama desteği gerçek cihazda doğrulanır; çekirdeğin bağımlılığı yapılmaz. |
| Varsayılan asistan | Kullanıcının seçtiği VoiceInteractionService sistem tarafından çalışır tutulabilir. | Asistan rolü ve sesle uyandırma ayrı cihaz denemesiyle eklenir. |
| Sürekli ses dinleme | Standart SpeechRecognizer sürekli tanıma için tasarlanmamıştır. Arka plan mikrofon kullanımının koşulları vardır. | İlk sürüm basarak konuşma; uyandırma sözcüğü için ayrı yerel algılama ve pil kabulü. |
| iPhone | App Intents ve Shortcuts uygulamanın açtığı işlemleri sistemle birleştirir. | Her Android işlevi iOS için yeniden kapsamlandırılır. |

Bu kararların dayanakları:
[Android ortak intent'leri](https://developer.android.com/guide/components/intents-common),
[Google Play Accessibility politikası](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en),
[AppFunctions](https://developer.android.com/ai/appfunctions),
[EXECUTE_APP_FUNCTIONS erişimi](https://developer.android.com/reference/android/Manifest.permission#EXECUTE_APP_FUNCTIONS),
[VoiceInteractionService](https://developer.android.com/reference/android/service/voice/VoiceInteractionService),
[SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer),
[arka plan mikrofon koşulları](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
ve [Apple App Intents](https://developer.apple.com/documentation/appintents).

Apple'ın iOS 26.2+ yan tuşla konuşma uygulaması başlatma seçeneği Japonya
koşullarına bağlıdır. Türkiye için aynı erişim varsayılmaz. Bu seçenek de
diğer uygulamalarda sınırsız işlem yetkisi vermez.
[Apple'ın Japonya kapsamı](https://developer.apple.com/support/app-distribution-in-japan)

Geniş uygulama kontrolünde iki geliştirme yolu vardır. Genel dağıtım için
resmî API, intent, uygulama bağlantıları ve erişim verilirse AppFunctions
önerilir. Kişisel kullanım APK'sında, kullanıcının açtığı ve durdurabildiği
ekran kontrolü ayrı deneysel mod olarak değerlendirilebilir: yalnız
desteklenen uygulamalarda görünür ekranı okuyup işlem yapma, her adımın
sonucunu kontrol etme ve gönderim gibi etkilerde onay. Bu modun teknik
temeli AccessibilityService olabilir; özel APK dağıtımı Android erişim
sınırlarını kaldırmaz ve bu mod Google Play kabulü varmış gibi sunulmaz.
[Android ekran okuma ve etkileşim API'si](https://developer.android.com/guide/topics/ui/accessibility/service)

## 3. İlk sürümün görev kapsamı

| Kullanıcı isteği | Planlanan davranış | Tamamlanma ve erişim sınırı |
| --- | --- | --- |
| “Annemi ara.” | Kullanıcının belirlediği kişi eşlemesini çözer; numarayı gösterir, izin ve arama tercihi uygunsa doğrudan aramayı başlatır. | Kişi belirsizse seçim; izin yoksa arama ekranına aktarım. Görüşmenin gerçekleştiği iddia edilmez. |
| “Yarın sabah yediye alarm kur.” | Yerel tarih/saat ve tekrar bilgisini çözer; sistem Saat uygulamasına alarm isteği verir. | Intent gönderildi diye alarm kuruldu sayılmaz; sonuç geri bildirimi yoksa Saat uygulamasında kontrol yolu sunulur. |
| “WhatsApp'ı aç.” | Yüklü uygulamayı açar. | Uygulama yoksa belirtir; uygulama açılışı içeride işlem yapma yetkisi değildir. |
| “Ahmet'e on dakika gecikeceğimi yaz.” | Alıcıyı ve metni çözer, mesaj taslağı oluşturur. | SMS/mesaj uygulamasında kullanıcı gönderir; desteklenen API bağlantısında ayrı gönderim onayıyla işlem yapılabilir. |
| “En yakın eczaneyi bul.” | İzinli konum veya kullanıcının verdiği semtle arama yapar; kaynak ve harita bağlantısı sunar. | Nöbetçi/çalışma saati gibi değişken bilgi ayrıca güncel kaynaktan doğrulanır. |
| “Bu adresin yol tarifini aç.” | Uygun harita uygulamasında hedefi açar. | Harita açıldı sonucu verilir; yolculuk tamamlandı sayılmaz. |
| “Yarın üçe toplantı ekle.” | Başlık, tarih, süre ve saat dilimiyle takvim taslağı hazırlar. | İlk sürüm sistem takviminde kullanıcı kaydeder; API/Calendar Provider yolu eklenince kayıt kimliğiyle doğrulanır. |
| “Bunu not al.” | Uygulama içinde not oluşturur. | Yerel kayıt okunarak doğrulanır; çevrimdışı çalışabilir. |
| “Seçtiğim PDF'yi özetle.” | Kullanıcının seçtiği dosyadan içerik çıkarır ve özet hazırlar. | Buluta aktarılacak içerik için açık veri tercihi uygulanır; tüm dosyalara erişim alınmaz. |
| “Son bildirimlerimi özetle.” | Kullanıcının açtığı bildirim erişiminden, seçili uygulamaların uygun bildirimlerini işler. | İlk MVP sonrasında; hassas kodlar ayıklanır, eski sohbet geçmişini okuyabildiği varsayılmaz. |
| “İnternette bunu araştır ve karşılaştır.” | Arama, kaynak okuma ve sonuç karşılaştırmasını yapar. | Canlı arama servisi kullanılır; modelin tahmini arama sonucu sayılmaz. |
| “Bunun fotoğrafını açıkla.” | Kullanıcının çektiği/seçtiği görüntüyü analiz eder. | Kamera/galeri seçimi kullanıcıdan; cihaz veya bulut modeli yeteneğine bağlı. |

İlk MVP: sesli giriş/yanıt, arama, uygulama açma, alarm, harita, not,
web araştırması, mesaj/takvim taslağı ve sınırlı çok adımlı görev.
Bildirim özetleri, dosya/görsel analizi, e-posta bağlantısı ve rutinler
takip sürümleridir.

Tam SMS gönderimi ve SMS geçmişi ayrı kapsamdır. Google Play'in varsayılan
SMS/telefon/asistan rolü ve izin onayı koşulları incelenmeden bu izinler
istenmez. CALL_PHONE ile arama başlatma, READ_CALL_LOG erişimiyle karıştırılmaz.
[SMS ve arama kaydı politikası](https://support.google.com/googleplay/android-developer/answer/10208820?hl=en)

## 4. Agent nasıl çalışacak?

Örnek görev: “Yarın Kadıköy'de yağmur var mı bak, yanıma alacaklarımı listele
ve hazırlığı bana hatırlat.”

1. Yarın tarihini ve konumu kesinleştirir.
2. Güncel hava kaynağını okur.
3. Sonuca göre hazırlık listesini uygulamada kaydeder.
4. Hatırlatma saati eksikse yalnız o bilgiyi ister.
5. Hatırlatma kaydını ve elde edilen sonucu kontrol eder.
6. Hangi adımın tamamlandığını, hangisinin kullanıcı işlemi beklediğini gösterir.

İkinci örnek: “Paylaştığım belgeden toplantı gündemi çıkar, takvim taslağını
hazırla ve ekibe göndereceğim e-postayı yaz.” Belge okuma, gündem, takvim
taslağı ve e-posta taslağı birbirine bağlanır. Gönderim, bağlı hesabın
desteklediği API üzerinden alıcı/metin önizlemesi onaylandıktan sonra yapılır.

### Yürütme zinciri

~~~mermaid
flowchart LR
    A["Ses veya yazı"] --> B["Niyet ve görev planı"]
    B --> C["Cihazdaki izin ve doğrulama kapısı"]
    C --> D["Android işlemi veya bağlı hizmet API'si"]
    D --> E["Sonucu kontrol et"]
    E --> F["Kısa sesli yanıt ve görev kartı"]
    E --> B
~~~

Basit ve kesin komutlar cihazdaki doğrulanmış komut yolundan yürür.
Karmaşık görevlerde araç çağrısı destekleyen model plan önerir.
Model, telefon iznini veya gönderim onayını kendisi veremez.

Araç kayıtları ad, parametre şeması, gerekli izin, veri kapsamı, işlem etkisi,
zaman aşımı, tekrar davranışı ve sonuç doğrulama yöntemini içerir.
İlk araçlar: kişi çözümleme, arama başlatma, uygulama açma, alarm isteği,
harita açma, not oluşturma, web arama ve mesaj/takvim taslağı.

Görevler alındı → bilgi bekliyor → planlandı → izin/onay bekliyor →
çalışıyor → kullanıcıya aktarıldı / doğrulandı / başarısız / iptal
durumlarıyla saklanır. Aktarım terminal başarı olarak yazılmaz.

Her görev için adım, süre ve harcama bütçesi bulunur; ilk deneyde en fazla
8 araç adımı ve sınırlı okuma tekrarı önerilir. Yazma işlemleri işlem kimliğiyle
tekrarlanmaya karşı korunur. Gönderim zaman aşımında durumu doğrulamadan
aynı mesaj tekrar gönderilmez.

Kullanıcının “Dur” komutu kuyruktaki adımları keser. Tamamlanmış dış işlemler
geri alınabiliyorsa ayrı bir geri alma yolu sunulur; iptal edilmiş görev
daha sonra kendiliğinden devam etmez.

## 5. Giriş, konuşma ve hızlı kullanım

- İlk sürüm: basılı tutarak konuşma, yazılı giriş ve konuşma metnini düzeltme.
- Ana ekran widget'ı ve uygun cihazda sistem asistan kısayolu.
- İkinci aşama: kullanıcı seçerse varsayılan dijital asistan rolü.
- “Hey Kutarya”: ayrı yerel uyandırma algılayıcısı ve cihaz/pil denemesi.
- Sesli yanıt, kullanıcı araya girdiğinde durur; arama, kulaklık, ekran kilidi
  ve uygulama arka plan geçişleri işlenir.

Türkçe cihaz içi ses tanıma varsa kullanılır. Android 12+ üzerinde ilgili
API ve Türkçe dil desteği ayrıca kontrol edilir. Yerel destek yoksa kullanıcı
çevrimiçi ses seçeneğini açıkça seçebilir veya yazıyla devam eder.
Sessizce buluta geçilmez. TTS motorunda Türkçe ve çevrimdışı ses desteği
cihazda ölçülür.

Tek asistan oturumunun kesintisiz konuşması ile bütün gün mikrofonun açık
kalması ayrı özelliklerdir. Arka plan servisi izinleri ve pil maliyeti,
uyandırma özelliği kabul edilmeden ölçülür.

## 6. Model ve altyapı seçimi

**Öneri: cihazda hızlı telefon işleri + bulutta güçlü görev planlama.**

| Parça | Önerilen uygulama |
| --- | --- |
| Android istemci | Android Studio, Kotlin, Jetpack Compose, Coroutines ve ViewModel |
| Yerel veri | Room/SQLite: görevler, notlar, sohbetler, kullanıcı tanımlı kişi eşlemeleri |
| Güvenli saklama | Android Keystore ile anahtar/token şifreleme; kişisel içerik için şifreli saklama tasarımı |
| Telefon araçları | Native Android API'leri ve doğrulanmış intent'ler |
| Bulut agent | Araç çağrısı destekleyen model; sağlayıcı değişebilir bir arayüz |
| Sunucu | Ayrı kapsamlı Cloud Run veya Firebase Functions; sahibin PC'sine bağlı çalışma yok |
| Hesap ve kota | Sunucuda doğrulanan oturum, kullanıcı/cihaz kotası ve harcama sınırı |
| Bağlı hizmetler | Gerekli en dar OAuth kapsamları; kullanıcıdan iptal edilebilir bağlantı |
| Sonraki yerel model | Cihazda Türkçe ve araç doğruluğu ölçülerek seçilen küçük model |

Compose yeni Android arayüzleri için resmî öneridir; mevcut Java modülleri
uygun arayüzlerle tekrar kullanılabilir.
[Android mimari önerileri](https://developer.android.com/topic/architecture/recommendations)

İlk sürüm için sıfırdan model eğitimi gerekmiyor. Mevcut küçük Kutarya
checkpoint'lerinin Türkçe görev ve araç başarısı kanıtlanmadan agent
yürütücüsü olarak seçilmesi önerilmez. Kutarya Us ürün kimliğiyle çalışan
model sağlayıcısı uygulama mimarisinden ayrılır.

Kullanıcıya API anahtarı girmeyi zorunlu kılmayan üretim sürümünde sağlayıcının
sahibe ait anahtarları APK'ya konulmaz; bulut gizli bilgi deposunda tutulur.
Geliştirici/BYOK prototipi ayrı seçenek olabilir. Bu seçenek de gömülü
ortak anahtar kullanmaz.

Ses/yanıt akışı için gerçek bulut yolunda gecikme ölçülür. Üst DEVIR.md'deki
Hosting rewrite tamponlama tuzağı nedeniyle mobil streaming, doğrulanmış
doğrudan HTTPS servis yolundan tasarlanır. Eski PC tüneli ürün bağımlılığı
yapılmaz.

Hesap senkronizasyonu gerektiğinde ortak Kutarya hesabı değerlendirilebilir;
mevcut e-posta ve reset akışı önce incelenir. Katalog admin yetkisi mobil
agent yetkisi olarak kullanılmaz; mobil veriler ayrı API/koleksiyon
kapsamında tutulur. Plan, web ziyaretçi hesabının yeniden açılmasını gerektirmez.

## 7. Mevcut Kutarya kodundan yararlanma

Bu görevde yerelde görülen kaynak:
mobile-src/kutarya-cep/app/src/main/java/com/kutarya/cep.

- ProviderApi: sağlayıcı bağlantısı ve yanıt akışı için aday.
- KeyVault: Keystore ile anahtar saklama yaklaşımı için aday.
- ChatDb: geçmiş ve hafıza davranışları için aday.
- Mevcut ses/yanıt deneyimi ve gerçek uygulama görselleri ürün incelemesinde
  değerlendirilebilir.

Kutarya Cep Java/SQLite/BYOK sohbet uygulamasıdır; tam telefon agent'i
hazırmış gibi ele alınmaz. Bu kaynaklar yeniden incelenerek uygun parçalar
alınır; mevcut uygulama yeniden adlandırılmaz veya bozulmaz.

Önceki kayıtta geçen D:/KutaryaVsJarvis/app/build.gradle.kts ve DEVIR.md
bu görevde bulunamadı. Önceki Pocket-AI çalışması derleme/cihaz kabulü
tamamlanmış bir temel sayılmaz; kullanılması düşünülürse gerçek konumu ve
temiz derlemesi yeniden doğrulanmalıdır.

İlk geliştirme adımı, kullanılacak kaynak ve bağımsız paket kimliğini
netleştirmektir. com.kutarya.mobile yalnız önerilen kimliktir; mağaza ve
mevcut imza/uygulama kimlikleri doğrulanmadan kesinleştirilmez.

## 8. Güvenilir işlem ve veri tercihleri

| Durum | Davranış |
| --- | --- |
| Uygulama açma, genel arama, hesaplama | Açık kullanıcı isteği yeterlidir. |
| Not/alarm gibi açık ve geri alınabilir istek | Bilgiler kesinse işler; her adımda yeniden onay sorulmaz. |
| Kişi, numara, tarih veya kapsam belirsizliği | Yalnız eksik bilgiyi ister. |
| Mesaj/e-posta gönderimi, silme veya ücretli işlem | Etkilenecek kişi/içerik/bedelle işlem önizlemesi ve onay. |
| Kilitli telefon veya hassas veri | Uygun işlem için cihaz kilidi açılmasını ister. |
| Desteklenmeyen uygulama işlemi | Yapılabilir kısmı hazırlar ve kullanıcıya aktarır. |

İzinler ihtiyaç anında istenir. Rehberin tamamı veya bütün bildirimler
varsayılan olarak buluta gönderilmez. Yerelde kişi kimliğiyle çalışılır;
modelin ihtiyaç duyduğu veri azaltılır. Bildirimde parola/OTP gibi
içerikler hafızaya ve genel aramaya taşınmaz.

Web sayfası, PDF, e-posta ve bildirim içeriği dış veridir. İçlerindeki
“şu aracı çalıştır” metinleri kullanıcı talimatı sayılmaz.
Veri metni görev iznini değiştiremez ve yeni alıcı/hesap oluşturamaz.

Kullanıcı “beni hatırla” dediğinde tutulan bilgi Ayarlar/Hafıza bölümünde
görülebilir, düzenlenebilir ve silinebilir. Sohbet geçmişi otomatik model
eğitimi değildir. Kişisel veriler veya kullanıcı belgeleri eğitim setine
kendiliğinden katılmaz.

## 9. Arayüz

Dört ana alan:

1. **Asistan:** büyük mikrofon, son konuşma, hızlı işlem önerileri.
2. **Görevler:** devam eden/tamamlanan işler, eksik bilgi ve işlem kanıtı.
3. **Rutinler:** kullanıcı tarafından oluşturulan tekrar kullanılabilir akışlar.
4. **Ayarlar:** ses, izinler, bağlantılar, hafıza ve veri tercihleri.

Dinliyorum / İşliyorum / İşlem bekleniyor / Tamamlandı durumları kısa,
okunaklı kartlarla gösterilir. Animasyonlar durum bilgisini destekler;
sesli geri bildirim kapatılabilir. Açık/koyu/sistem tema, büyük yazı,
TalkBack ve azaltılmış hareket seçenekleri tasarıma dahildir.

“Günümü hazırla” rutini hava durumu + seçili takvim + yapılacakları
birleştirebilir. Kullanıcı önce rutinin adımlarını görüp kaydeder.
WorkManager senkronizasyon işleri için kullanılır; dakikası kesin alarm
olarak kabul edilmez. Zamanında hatırlatma ayrı Android alarm/bildirim
koşullarıyla tasarlanır.

## 10. Geliştirme sırası ve tahmini süre

Tam zamanlı bir geliştirici, erişilebilir test cihazları ve mevcut AI
servislerine erişim varsayımıyla:

| Aşama | Çalışma | Somut çıktı |
| --- | --- | --- |
| 0 — ilk 3–5 iş günü | Cihaz/kaynak envanteri; arama, ses, alarm, asistan rolü ve AppFunctions erişimi için küçük denemeler | Çalışan/yönlendirilen/desteklenmeyen işlem matrisi ve kaynak kararı |
| 1 — ilk 1–2 hafta | Native ekran, basarak konuşma, Türkçe yanıt, ilk gerçek telefon araçları | Fiziksel telefonda erken demo |
| 2 — toplam 4–6 hafta | MVP görevleri, sınırlı agent döngüsü, sonuç kontrolü, iptal ve izin/onay | Denenebilir Android APK ve görev kabul raporu |
| 3 — toplam 10–14 hafta | Bildirim/dosya/görsel, e-posta/takvim bağlantıları, rutinler, asistan rolü, pil ve hata iyileştirmeleri | Kullanıcı pilotuna uygun daha geniş Android beta |
| 4 — ayrı kapsam | Yerel model, koşullu AppFunctions, kişisel APK için deneysel ekran kontrolü ve iPhone istemcisi | Cihaz/erişim kanıtına göre yeni sürüm planı |

Bu aralıklar gerçekleşmiş süre veya teslim garantisi değildir.
Mağaza incelemesi, servis başvuruları, API izinleri ve iPhone geliştirmesi
Android MVP tahminine dahil değildir.

Her aşama önceki aşamanın gerçek cihaz kabulünden sonra genişler.
Uygulama açılmasını, internet modelinden gelen sohbet cevabını veya bir
emülatör ekranını tüm agent sisteminin tamamlanması sayma.

## 11. Kabul hedefleri

İlk aşamada sürümü sabitlenen en az 150 Türkçe istekten oluşan değerlendirme
seti hazırlanır: kişi/ad/saat belirsizliği, aynı isimli kişiler, “ara”
kelimesinin farklı anlamları, farklı telaffuzlar ve çok adımlı görevler.

- Desteklenen tek adımlı işlemlerde en az %95 doğru görev sonucu hedefi.
- Seçili çok adımlı görevlerde en az %90 sonuç hedefi.
- Test setinde onaysız hassas işlem ve kanıtsız başarı bildirimi: 0 hedefi.
- Yerel komutlarda konuşma bittikten sonraki işlem gecikmesi; bulut görevlerinde
  ilk kullanılabilir yanıt ve toplam tamamlanma süresi ayrı ölçülür.
- İlk gecikme hedefi: yerel işlemlerde çoğu istekte 1–2 saniye; basit bulut
  yanıtında uygun ağda 2–4 saniye. Bunlar ölçülmüş performans değildir.
- En az iki üreticiden üç fiziksel Android cihazı; düşük/orta/yüksek donanım
  ve güncel/eski desteklenen Android sürümleri.
- Uçak modu, mikrofon/rehber/konum reddi, bağlantı kopması, API kota hatası,
  süreç kapanması ve yeniden açılması.
- Ekran kilidi, kulaklık, gelen telefon araması, büyük yazı ve TalkBack.
- Uyandırma özelliğinde yanlış tetikleme, Türkçe algılama, pil ve ısınma ölçümü.

Birim testleri parametre doğrulama ve izin kapısını; cihaz testleri gerçek
Android işlemlerini; canlı servis kabulü gerçek API sonuçlarını kontrol eder.
Bu üç kanıt ayrı raporlanır.

## 12. Maliyet ve dağıtım

Yerel telefon araçları model çağrısı gerektirmeyebilir. Bulut kullanımında
metin/model, ses tanıma/üretme, arama/harita servisleri ve sunucu gideri ayrı
ölçülür. İlk sürümde sürekli bulut ses oturumu maliyet bağımlılığı önerilmez.

Aylık tahmin = aktif kullanıcı × günlük görev × ay içindeki kullanım günü
× buluta giden görev oranı × ortalama görev maliyeti + altyapı gideri.

Gerçek görev başı maliyet pilotta ölçülür; kullanıcı ve toplam hizmet
bütçeleri sunucuda uygulanır. Güncel fiyatlar ve mağaza ücretleri
yayın kararında resmî kaynaklardan alınır; bu belgede fiyat tahmini yapılmadı.

İlk teslim test APK'sı; sonra üretim imzalı APK/AAB ve kapalı test.
Mağaza yayını için izin bildirimleri, gizlilik/veri beyanları ve gerçek cihaz
kabulü tamamlanır. Kutarya.com'a ürün/paket ekleme ve Hosting yayını,
mobil uygulama geliştirmesinden ayrı yayın adımlarıdır.

## 13. Bu planla verilen karar

Başlangıç paketi: **Android native, Türkçe sesli etkileşim, cihazda telefon
araçları, bulutta görev planlama, ölçülebilir sonuç kontrolü.**

İlk iş fiziksel telefonda “annemi ara”, “alarm kur”, “uygulamayı aç” ve
“internette bul” akışlarının çalışmasını göstermek; sonra bu araçları
çok adımlı görevlerde birleştirmektir.

Bu görevde yalnız plan ve devir belgeleri hazırlandı. Mobil kod, APK, model
eğitimi, API ücretli isteği, uygulama kurulumu, Firebase değişikliği veya
site/mağaza yayını yapılmadı.
