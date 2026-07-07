# galarm ⏰

**galarm**, güne zinde, odaklanmış ve hızlı bir şekilde başlamanızı sağlayan, "atlatılamaz" (un-bypassable) tasarıma sahip modern bir Android bulmaca alarm uygulamasıdır. Klasik alarmların aksine erteleme seçeneği barındırmaz ve alarmı kapatabilmeniz için beyninizi uyandıracak bulmacaları çözmenizi şart koşar.

---

## 🚀 Öne Çıkan Özellikler

### 1. Atlatılamaz Tasarım (No-Snooze)
- Uygulamada **Erteleme (Snooze) özelliği bulunmaz**. Geri tuşu gibi standart navigasyonlar devre dışıdır. Alarmı susturmanın tek yolu, atanan görevi başarıyla tamamlamaktır.

### 2. Akıllı Bulmaca Görevleri (Puzzles)
Uygulamada her alarm kurulurken kolay, orta veya zor derecelerde ayarlanabilen, her seferinde **dinamik algoritmalarla farklı şekillerde üretilen** 6 bulmaca türü bulunur:
- ⚡ **Zip Oyunu:** Grid üzerindeki tüm hücreleri tek bir çizgiyle tam olarak 1 kez kaplayıp sayıları sırayla bağlamayı hedefleyen Hamiltonian yolu çizim bulmacası (LinkedIn Zip esintili).
- 🎨 **Renk Testi (Stroop):** Kelime anlamı ile kelime renginin çeliştiği psikolojik Stroop testi. Dikkat ve odaklanmayı ölçer.
- 🔢 **Sayı Sıralama:** Grid üzerine karışık yerleştirilmiş sayıları sırayla bularak dokunma görevi.
- 🎴 **Hafıza Kartı:** Eşleşen emojileri bulup kartları eşleştirme görevi.
- 🔢 **Matematik:** Belirlenen zorluk seviyesine göre rastgele üretilen matematik denklemlerini çözme görevi.
- 📱 **Telefon Sallama:** Telefonu hızlıca sallayarak güç barını doldurma görevi.

### 3. Pil ve Performans Optimizasyonları
- **Yaşam Döngüsüne Duyarlı Sensörler:** Sallama oyununda kullanılan ivmeölçer sensörü, uygulama arka plana alındığında veya kilitlendiğinde otomatik olarak durdurulur (`onResume`/`onPause`). Arka planda gereksiz pil tüketimi yapmaz.
- **7 Dakikalık Otomatik Susturma:** Telefon başında olunmadığında alarmın saatlerce çalarak pili tamamen bitirmesini önlemek için 7 dakikalık otomatik zaman aşımı (auto-dismiss) mekanizması eklenmiştir.

---

## 🛠️ Teknolojiler ve Mimari

- **UI Framework:** Jetpack Compose (Modern ve bildirimsel UI)
- **Mimari:** Clean Architecture & MVVM (ViewModel, StateFlow)
- **Navigasyon:** Androidx Navigation3
- **Veri Depolama:** File-based JSON veritabanı & Kotlinx Serialization
- **Arka Plan:** Android Foreground Service & AlarmManager (Exact alarm desteği)

---

## 💻 Kurulum ve Derleme

Projeyi derlemek ve cihazınıza yüklemek için aşağıdaki komutları kullanabilirsiniz:

### Hata Ayıklama (Debug) Sürümü Derleme & Yükleme
```bash
# Debug APK oluşturur
./gradlew assembleDebug

# Bağlı emülatör veya cihaza yükler
./gradlew installDebug
```

### Yayın (Release) Sürümü Derleme
```bash
# Release APK oluşturur (app/build/outputs/apk/release/ altında yer alır)
./gradlew assembleRelease
```
