# Tempo

**YouTube Shorts** videolarını, seçtiğiniz bir zamanlayıcıyla elleri serbest kaydıran; her zaman çalışan bir Başlat/Durdur kontrolü sunan ücretsiz, açık kaynaklı Android yardımcısı. İngilizce: [README.md](README.md).

> Tempo, YouTube veya Google ile bağlantılı değildir ve onlar tarafından onaylanmamıştır. "YouTube", Google LLC'nin ticari markasıdır ve burada yalnızca uygulamanın birlikte çalıştığı şeyi anlatmak için kullanılır.
>
> Bu proje yapay zekâ desteğiyle geliştirildi ve insan tarafından gözden geçirildi.

**Durum: yayın öncesi (0.1.0).** Derleniyor; saf mantık ve arayüz birim testleriyle sınandı. Gerçek bir cihazda gerçek YouTube uygulamasına karşı **henüz denenmedi**; bkz. [docs/OPEN_ITEMS.md](docs/OPEN_ITEMS.md). Ad ve uygulama kimliği geçicidir.

## Ne yapar

- Sizin başlattığınız oturumlar: 3-120 sn aralık, 15-240 dk süre (sınırsız yok), isteğe bağlı kaydırma sınırı, yön.
- Ekrana dokunduğunuzda, yorumlar açıkken, Shorts akışından çıkınca ve büyük olasılıkla reklam varken duraklar.
- Durdur düğmeli kalıcı bildirim. Ekran kapanınca, YouTube'dan 60 sn uzak kalınca veya sınırlar dolunca oturum biter.
- Tanılama ekranı ve ekran içeriği içermeyen kopyalanabilir rapor.

## Ekstralar

- **Hareket:** telefonu sağa-sola ya da öne-arkaya hızlıca oynatın, sonraki Short'a geçer (varsayılan kapalı).
- **Reklamı geç:** isteğe bağlı “Atla'ya otomatik dokun” ayarı, görünür olduğu düşünülürse YouTube'un kendi Atla düğmesine dokunur. Yalnızca `foss` sürümünde, varsayılan kapalı.

## Kimin için

Öncelikle elleri serbest Shorts izlemek isteyen herkes için. Hareket, görme veya diğer fiziksel engeli olanlara da yardımcı olabilir (daha az kaydırma), ama asıl hedef kitle onlar değildir; uygulama erişilebilirlik aracı olarak beyan edilmemiş ve öyle sınanmamıştır. Bkz. [docs/ACCESSIBILITY_STATEMENT.md](docs/ACCESSIBILITY_STATEMENT.md).

## Asla yapmaz

Reklamların kendisine dokunmaz (yalnızca yukarıdaki biçimde YouTube'un kendi Atla düğmesine), otomatik beğeni/abonelik/yorum/izlenme üretmez, reklamları aşmaz, tespitten kaçınma hileleri yapmaz, kimlik bilgisi, geçmiş veya ekran içeriği toplamaz. Analiz yok, reklam yok; `foss` sürümünde ağ izni yok.

## Kurulum

1. APK'yı GitHub Releases'tan indirin ve `SHA-256` dosyasını doğrulayın.
2. Android erişilebilirlik hizmetini açmanıza izin vermezse **Uygulama bilgisi → ⋮ → Kısıtlı ayarlara izin ver** yolunu izleyin (seçenek, hizmeti bir kez açmayı denedikten sonra görünebilir).
3. Uygulama içi kurulumu izleyin: bilgilendirme, onay, hizmeti açma, bildirim izni.

## Derleme

```
export JAVA_HOME=/jdk17/yolu ANDROID_HOME=/Android/Sdk/yolu
./gradlew test lint assembleFossDebug
```

Lisans: Apache-2.0.
