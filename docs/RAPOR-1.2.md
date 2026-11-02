# Kutarya Mobile 1.2.0 — uygulamada neler var?

Geliştirme D:/KutaryaMobile üzerinde yapıldı. Paket com.kutarya.mobile,
Android 8+ tabanı. Asistan adı Kutarya Us. Yerel dil modeli kullanılmaz.
31 kayıtlı araç desteklenen telefon işleri ve kayıt işlemlerini yürütür.

- **Devam edilebilir konuşmalar:** Yeni istekte önceki mesajlar kaybolmaz.
  Konuşmaları arama, seçme, devam etme, yeni konuşma açma ve onayla silme.
  Son açık konuşma uygulama tekrar açılınca geri gelir. Eski görev kayıtları
  ayrı Eski kayıtlar grubunda korunur.
- **İsteğe bağlı AI bağlamı:** Açık konuşmanın en fazla altı uygun soru/yanıt
  çifti ve 12.000 karakteri kullanılır. Telefon işlemleri ve özel kayıtlar
  otomatik geçmiş aktarımına dahil edilmez. Dosya sonuçları paylaşım tercihi
  kapalıyken sonraki soruya gönderilmez. Önceki mesaj yeni işlem yetkisi vermez.
- **Yeni ana ekran:** Günlük plan kartı, gerçek kayıt sayıları, sıradaki
  hatırlatma, dört hızlı iş ve kayıtlı rutinlere kısayol. Açık/koyu/sistem
  görünümü; hareket azaltma seçeneği. Gereksiz ana sayfa API metinleri yoktur.
- **Günlük özet:** Bugünün kayıtlı hatırlatmaları, zamanı geçtiği hâlde teslim
  edildi olarak işaretlenmemiş kayıtlar ve listelerdeki açık madde sayısı.
  Kayıtlı olmayan bir program veya takvim etkinliği uydurulmaz.
- **Türkçe komutlar:** Kibar söyleyişler, Türkçe sayı/saat/süre, bilinen
  uygulama adlarının yaygın sesle yazılışları, eksik saat/süre için sonraki
  mesaj. Anlaşılmayan istekte ilgili komutlar önerilir; seçim yazı alanını
  doldurur, kendiliğinden işlem başlatmaz.
- **Ses girişi:** Android hizmeti desteklediğinde canlı kısmi metin ve
  mikrofon seviye çubukları. Bitince metni düzeltip gönderme; isteğe bağlı
  hemen gönderme. Yerel hizmet yoksa açık açıklama ve yazıyla kullanım.
  Türkçe seslendirme; çevrimiçi ses kullanımı ayrı tercihe bağlıdır.
- **Notlar:** Form veya komutla oluşturma, arama, okuma, düzenleme ve silme.
  Yanıtı not alma, yanıt kopyalama ve eski isteği yazı alanına geri getirme.
- **Listeler:** Formdan liste açma; madde ekleme, tamamlama, geri alma,
  tam madde adıyla onaylı kaldırma ve listeyi onaylı silme.
- **Hatırlatmalar:** Formda açık tarih/saat seçimi veya Türkçe zaman komutu.
  Bekleyenleri görüntüleme; onaydan sonra yerel zamanlamayı iptal etme.
  Android bildirim izni gerektiğinde istenir.
- **Rutinler ve görev planı:** En fazla sekiz desteklenen adımı kaydetme;
  geçersiz komut/parametreyi kaydetmeden gösterme. Adlarıyla çalıştırma;
  çalışan, sıradaki, tamamlanan veya başarısız adımları görüntüleme ve durdurma.
  Bir adım başarısız olursa sonraki etkiler çalıştırılmaz.
  Aynı adlı rutinler karttan seçildiğinde seçilen kayıt kimliği çalışır.
- **Telefon işleri:** Yüklü uygulama açma; rehber/numara ile onaylı arama
  ekranı veya seçili doğrudan arama isteği; mesaj/e-posta/takvim taslağı;
  harita araması; alarm ve sayaç isteği; donanım varsa el feneri;
  medya sesini ayarlayıp geri okuma; ilgili Android ayar ekranı açma.
- **Hesap yardımcıları:** Matematik, Türkçe sayı ifadeleri, birim ve sıcaklık
  çevirme, yüzde/indirim/zam ve tarih hesabı cihazda çalışır.
- **Metin ve dosyalar:** 200 KB'a kadar UTF-8 TXT/Markdown üzerinde
  kaynak cümlelerini seçerek özet, kelime/karakter sayımı ve satır arama.
  Dosyanın içindeki komutlar çalıştırılmaz. PDF/görsel analizi en fazla
  6 MB ek ve kullanıcının açtığı AI bağlantısıyla yapılır.
- **Bildirimler ve asistan girişi:** Açık tercih ve Android erişimiyle gelen
  uygun bildirimleri yerelde tutma/okuma; hassas kodları süzme.
  Android varsayılan asistan seçimi ve widget giriş noktası korunur.
- **Veri yönetimi:** Not/liste/rutin/hatırlatma/konuşma içerikleri AES-GCM ile
  şifrelenerek SQLite'ta, API anahtarı Keystore korumalı kasada saklanır.
  JSON kayıt yedeği çıkarma (her türün son 200 kaydı); API anahtarı ve
  konuşmalar bu dışa aktarıma girmez.
  Yedekten içe aktarma bu sürümde yoktur.
- **İsteğe bağlı genel AI:** Kullanıcının Gemini anahtarı, model listesi ve
  aktarım tercihleri. Genel sohbet, görsel/PDF yorumlama ve kaynaklı araştırma.
  Araç isimleri, parametreleri, niyet, kilit/izin/onay ve adım sınırları
  uygulamada ayrıca kontrol edilir.

## Kabul ve kapsam

Özelliklerin uygulamadaki karşılığı yukarıdadır. Gerçek koşu sonuçları,
APK kimliği ve cihaz sınırları KABUL-1.2.md'de kaydedilir.
Uygulama herhangi bir üçüncü uygulamanın bütün ekranlarını serbestçe
kontrol eden bir agent değildir. Sürekli “Hey Kutarya” dinleme ve iOS
sürümü yoktur. Genel üretken AI için bağlantı gerekir.

Fiziksel telefonda Türkçe ASR/TTS, rehber/hat, mesaj/takvim uygulamaları,
donanım feneri, widget ve Doze teslim süreleri ayrıca denenmelidir.
Canlı API hesabının kalite/kota/gecikme kabulü yapılmış sayılmaz.
Sonraki “Siteye yayınla” talimatıyla1.2, 10 Ekim'de
https://kutarya.com/proje/kutarya-mobile üzerinde yayımlandı.
Yeni APK/kaynak ve dört gerçek ekran mevcut sayfada; eski1.1 dosyaları
korunur. Site58/58 + build ve gerçek dört İndir düğmesi byte/SHA kabulü
geçti. Yayın raporu D:/Kutarya/KutaryaWeb/docs/KUTARYA-MOBILE-1.2-YAYIN.md.
