# Work Planner

Kişi bazlı iş takibi için geliştirilmiş, tamamen çevrimdışı çalışan bir Android uygulaması.
Her ekip üyesi bir kartla temsil edilir; kartın içinde bekleyen işler ve tamamlanan işler
(farklı renkte) listelenir. Bir işe dokunduğunuzda özet, ilerleme ve notların yer aldığı
detay sayfası açılır.

## Özellikler

- **İlk açılış onboarding'i** – uygulama kurulduktan sonra üç adımlı tanıtım ekranı
  açılır ve hatırlatıcılar için gereken izinler (bildirim, kesin alarm, tam ekran
  bildirim) tek tek istenir. İzinler daha sonra cihaz ayarlarından değiştirilebilir.
- **Kişi kartları** – ad soyad, yuvarlak profil fotoğrafı veya baş harf avatarı,
  kart başına ilerleme çubuğu. Kartın sağ üstündeki **⋮** menüsünden iş ekleme,
  fotoğraf seçme / kaldırma, yeniden adlandırma ve silme yapılır.
- **Profil fotoğrafı** – avatara dokununca galeri açılır; seçilen görsel
  sürükle-yakınlaştır ile yuvarlak olarak kırpılır ve kişi fotoğrafı olarak kaydedilir.
  Eski sürümde siyah kaydedilmiş avatarlar baş harflere düşer; fotoğrafın yeniden
  seçilmesi gerekir.
- **PDF raporu / e-posta** – ana ekranın sol altındaki **PDF** düğmesi kişileri ve
  üzerlerindeki tüm işleri A4 rapor olarak oluşturur; açılan paylaşım ekranından
  e-posta uygulaması seçilerek PDF ek olarak gönderilir.
- **JSON yedekleme / geri yükleme** – alt menünün ortasındaki yedekleme düğmesiyle
  tüm kişiler, işler, notlar, hatırlatıcılar ve fotoğraf yolları tek bir JSON
  dosyasına kaydedilir. Geri yükleme, dosyayı doğruladıktan ve onay istedikten
  sonra mevcut verilerin tamamını değiştirir; hatırlatıcılar yeniden kurulur.
- **Bekleyen / tamamlanan işler** – her işin yanında checkbox ve **Görev Sil**
  (çöp kutusu) düğmesi; işaretlenince iş "Tamamlandı" bölümüne taşınır ve
  *italik*, üstü çizili, soluk renkte gösterilir. Silinen işler için "Geri al" vardır.
- **İş detay sayfası** – özet, ilerleme yüzdesi (kaydırıcı), notlar, oluşturulma /
  tamamlanma zamanı. Düzenlemeler geri tuşunda otomatik kaydedilir.
- **Hatırlatıcı / alarm** – her iş için tarih-saat seçilebilir, değiştirilebilir veya
  kaldırılabilir. Zamanı gelince bildirim gönderilir; bildirim **kilit ekranında bile
  tam ekran** açılır ve işi açma, 10 dakika erteleme, tamamlandı olarak işaretleme
  seçenekleri sunar. Cihaz yeniden başlatıldığında alarmlar yeniden kurulur.
- **Kişi ekleme** – ekip başlığının sağındaki **+** düğmesi; isim sorar, ayrıca
  kişi kartından yeniden adlandırma ve silme yapılabilir.
- **İş ekleme** – ekranın sağ altındaki **İş Ekle** düğmesi; kişi seçimi, başlık ve
  isteğe bağlı hatırlatıcı.
- **Özet kartı** – "Ekip Görev Dashboard" başlığı altında toplam / bekleyen /
  tamamlanan iş sayıları ve genel ilerleme.
- **Tema** – krem arka plan üzerine zeytin yeşili (olive) palet, açık ve koyu mod.

## Teknoloji

| Bileşen | Sürüm |
| --- | --- |
| Dil | Kotlin 1.9.22 |
| UI | Jetpack Compose (BOM 2024.02.00), Material 3 |
| Mimari | `ViewModel` + `StateFlow`, tek `Activity` + Navigation Compose |
| Derleme | AGP 8.2.2, Gradle 8.6 |
| Min / Target SDK | 24 / 34 |

Veriler `SharedPreferences` içinde tek bir JSON belgesi olarak saklanır
(`LocalWorkRepository`), bu sayede harici veritabanı bağımlılığı yoktur. Profil
fotoğrafları uygulamanın özel dizininde (`files/avatars/`) yuvarlak PNG olarak tutulur.

## Proje yapısı

```
app/src/main/java/com/workplanner/app/
├── MainActivity.kt              # giriş noktası, onboarding kapısı, bildirim yönlendirmesi
├── WorkPlannerApplication.kt    # bildirim kanalı kurulumu
├── data/                        # modeller, repository, JSON kalıcılığı, fotoğraf depolama
├── notifications/               # AlarmManager, receiver'lar, bildirimler, tam ekran uyarı
└── ui/
    ├── WorkPlannerApp.kt        # navigasyon
    ├── WorkViewModel.kt         # durum ve hatırlatıcı senkronizasyonu
    ├── onboarding/              # ilk açılış akışı ve izinler
    ├── home/                    # ana pano, kişi kartları, diyaloglar
    ├── task/                    # iş detay ekranı
    ├── reminder/                # kilit ekranı hatırlatıcı ekranı
    ├── common/                  # avatar, fotoğraf kırpma, tarih biçimleri, saat seçici
    └── theme/                   # renkler, tipografi, tema
```

## Derleme

### Android Studio

Projeyi Android Studio ile açıp **Run ▶** tuşuna basmanız yeterli. İlk açılışta
Gradle bağımlılıkları indirilir.

### Komut satırı

```bash
tools/build.sh                 # debug APK üretir
```

veya doğrudan Gradle ile:

```bash
JAVA_HOME=/yol/jdk ./gradlew :app:assembleDebug
```

Çıktı: `app/build/outputs/apk/debug/app-debug.apk`

> **Not (exFAT disk):** Proje, macOS'un AppleDouble `._*` dosyaları oluşturduğu bir
> USB/exFAT diskte duruyorsa AGP'nin dex birleştirme adımı
> ("Unable to delete directory …", "Failed to process …") hata verebilir.
> `tools/build.sh` bu durumda dosyaları temizler ve derleme çıktısını otomatik olarak
> APFS üzerindeki geçici bir dizine taşır; APK yine `app/build/outputs/apk/debug/`
> altına kopyalanır. Elle yapmak isterseniz:
>
> ```bash
> WORKPLANNER_BUILD_DIR="$TMPDIR/workplanner-build" \
>   ./gradlew --project-cache-dir "$TMPDIR/workplanner-cache" :app:assembleDebug
> ```

### Kurulum

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Hazır debug APK: `dist/WorkPlanner-debug.apk` (debug anahtarıyla imzalanmıştır,
yalnızca test içindir).

## Hatırlatıcılar hakkında

- Android 13+ için ilk açılışta onboarding ekranında bildirim izni istenir.
- Android 12+ üzerinde "Alarmlar ve hatırlatıcılar" izni verilmemişse uygulama
  kesin alarm kuramaz; bu durumda yaklaşık zamanlı alarm kullanılır.
- Android 14+ üzerinde tam ekran hatırlatıcı için **Tam ekran bildirimlere izin ver**
  ayarı gerekir. İzin verilmezse uyarı yüksek öncelikli bildirim olarak görünür.
- Hatırlatıcı, `AlarmManager` + `ReminderReceiver` + `ReminderActivity` üzerinden
  çalışır. Ekran kapalıyken bile `showWhenLocked` / `turnScreenOn` ile açılır.
- Bildirim kanalı: **İş hatırlatıcıları**.

## Uygulama simgesi

Simge ve uygulama içi marka görseli, kök dizindeki `logo.png` dosyasından
`tools/generate_icons.py` ile üretilir (Pillow gerekir):

```bash
python3 -m venv .venv && .venv/bin/pip install pillow
.venv/bin/python tools/generate_icons.py
```

Betik; takvim/yaprak motifini logodan ayırır, arka planını saydamlaştırır ve
uyarlamalı simge ön planı (`drawable-nodpi/ic_launcher_foreground.png`), Android 13
tema simgesi (`ic_launcher_monochrome.png`), uygulama içi logo
(`drawable-nodpi/ic_logo_mark.png`), eski Android sürümleri için PNG mipmap'ler,
bildirim simgeleri ve `docs/icon_preview.png` önizlemesini yeniden oluşturur.
