# Kutarya Mobile 1.2 — ürün değerlendirmesi ve uygulama planı

Kullanıcı mevcut deneyimi yetersiz buldu; önce düşünme, plan, uygulama ve
iş sonunda uygulamanın içeriğini madde madde raporlama istedi.

## Kaynakta görülen sorunlar

- Ana ekran yalnız son isteği/yanıtı gösteriyor; yeni istekte eski yanıt kayboluyor.
- Görev kayıtları şifreli ama konuşma grupları ve devam ettirme yok.
  Genel AI'ya her görevde yalnız yeni istek gidiyor.
- Türkçe komutlar az sayıda sabit ifadeye bağlı. Desteklenmeyen isteğin
  nasıl düzeltileceği açıklanmıyor; ilgili komutlar önerilmiyor.
- Ses kısmi sonuçları ve mikrofon seviyesi yok. Sonuç otomatik yürütülüyor,
  hizmetin oluşturulmasındaki hata yakalama eksik. Ses hatası eski mesaj
  varken görünmeyebiliyor.
- Kayıtlarda yeni not/liste/hatırlatma oluşturma düğmesi yok; listeye elle
  madde ekleme/silme ve liste silme akışı eksik.
- Rutin kaydetmeden komutların geçerliliği ve adım planı gösterilmiyor.

## Olması gereken deneyim

Kutarya Us ile devam edilebilir bir konuşma; ne yapılacağını ve gerçek
sonucu açıkça gösteren görevler; sesli/yazılı kullanım; kayıtları doğrudan
yönetme; işlem tamamlanamadığında kullanılabilir sonraki seçenekler.

1. Aynı şifreli veritabanında konuşma kimlikleri ve geriye uyumlu görev
   alanları. Konuşma seçme/devam/silme; son konuşmayı yeniden açma.
2. Genel AI için yalnız aynı konuşmanın sınırlı bağlamı. Araç yetkisi hâlâ
   yeni isteğe bağlı; özel telefon/dosya sonuçları paylaşım tercihi olmadan
   geçmişten modele gönderilmez. Eski kayıtlar varsayılan aktarılmaz.
3. Güvenli Türkçe ifade eşleştirme, ilgili örnek önerileri ve eksik bilgi
   akışı. Not/mesaj gövdesindeki kelimeler komut diye değiştirilmez.
4. Daha kompakt ana ekran, kaybolmayan mesajlar, günlük özet ve hızlı
   işler; görev adımları, kopyalama/not alma/yeniden kullanım.
5. Sesle canlı metin ve seviye, son metni düzeltip gönderme; isteğe bağlı
   doğrudan ses komutu. Yerel ses yoksa açık hizmet bilgisi ve yazı yolu.
6. Not/liste/hatırlatma formları; listeye madde ekleme/kaldırma ve listeyi
   onayla silme. Rutin planını kaydetmeden doğrulama ve adımları gösterme.
7. Mevcut komut/izin/agent testleri ve yeni bağlam/güvenlik/UI kabulü;
   lint, debug/release, aynı imzalı 1.1 → 1.2 veri koruma, çevrimdışı kabul.

## Sınırlar ve teslim

Yerel LLM eklenmez; isteğe bağlı mevcut AI bağlantısı korunur. Paket/API/
Keystore anahtarı ve eski veriler korunur. Çalışma D:/KutaryaMobile'dadır.
1.1 yayımlanmış dosyalarının üzerine yazılmaz. Yeni sürüm1.2.0/code3 olarak
imzalı APK, kaynak ve özellik/kabul raporu hazırlanır. Canlı siteye yeni
sürüm yayını bu geliştirme talebinin parçası olarak varsayılmaz.
Fiziksel telefonun Türkçe ses/fener/hat testleri yapılmadan doğrulandı denmez.
