# Kutarya Mobile 1.1 geliştirme kapsamı

10 Ekim 2026. Kullanıcı yerelde daha güçlü çalışma, yerel model kullanmama,
tasarım iyileştirmesi, gereksiz metinlerin kaldırılması ve yeni APK istedi.

- Mevcut Kotlin/Compose projesi, veritabanı ve imza korunur.
- Yerel dil modeli eklenmez. Türkçe komut çözümleme, konuşma bağlamı,
  hesaplama ve metin işlemleri sınırlı algoritmalarla cihazda çalışır.
- Not arama/düzenleme, hatırlatma listeleme/iptal, işaretlenebilir listeler,
  ses/fener ve Android ayarlarına giriş eklenir.
- Birim/yüzde/tarih hesabı, yazıyla matematik ve UTF-8 metin dosyalarında
  cümle seçerek özet/arama yerelde yapılır. Bu özet üretken model değildir.
- Desteklenen çok adımlı cümleler ve kaydedilen rutin adıyla çalıştırma
  API istemez. Bilinmeyen veya eksik adım varsa kısmi işlem yapılmaz.
- Ana ekranda API kurulumu ve teknik durum metinleri kaldırılır. Yanıtlar,
  kayıtlar ve rutinler sadeleştirilir; gerekli işlem/aktarım onayları korunur.
- Genel AI bağlantısı isteğe bağlı Ayarlar içinde korunur; kendiliğinden
  anahtar eklenmez veya ücretli sağlayıcı denemesi yapılmaz.
- JVM testleri, özel Android emülatöründe gerçek UI/yerel kayıt kabulü,
  lint, yeni release imzası ve önceki sürümden veri koruyan güncelleme
  doğrulanır. Gerçek telefon/ses/hat kabulü ayrıca açık kalır.
