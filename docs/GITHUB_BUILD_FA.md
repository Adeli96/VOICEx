# ساخت APK با GitHub (خودکار، بدون Android Studio)

## ۱) ساخت مخزن و آپلود پروژه
1. در github.com یک مخزن جدید بساز (مثلاً `voicex`). Private یا Public فرقی ندارد.
2. پوشه‌ی پروژه (همین‌جا) را در مخزن بریز:
   - روش ساده: در صفحه‌ی مخزن «Add file ← Upload files» و همه‌ی فایل‌ها را بکش و رها کن (پوشه‌ی `.github` را هم حتماً با پوشه‌ها آپلود کن).
   - روش خط فرمان:
     ```
     git init && git add . && git commit -m "VoiceX"
     git branch -M main
     git remote add origin https://github.com/USERNAME/voicex.git
     git push -u origin main
     ```

## ۲) گرفتن APK
1. تب **Actions** ← workflow به نام «Build VoiceX APK» خودکار شروع می‌شود (حدود ۵ تا ۱۰ دقیقه).
2. وقتی سبز شد، وارد همان اجرا شو و پایین صفحه از **Artifacts** فایل `VoiceX-APK` را دانلود کن.
3. داخل zip فایل `VoiceX-debug.apk` است؛ آن را روی گوشی نصب کن (اجازه‌ی «نصب از منبع ناشناس» را بده).

اگر قرمز شد: روی مرحله‌ی قرمز کلیک کن و متن خطا را برای من بفرست تا درست کنم.

## ۳) نسخه‌ی امضاشده‌ی Release (اختیاری، برای انتشار)
1. یک keystore بساز (فقط یک بار، و آن را جایی امن نگه دار؛ گم‌کردنش یعنی دیگر نمی‌توانی آپدیت بدهی):
   ```
   keytool -genkeypair -v -keystore voicex.jks -alias voicex -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 voicex.jks > voicex.jks.b64      # در مک: base64 -i voicex.jks
   ```
2. در مخزن: Settings ← Secrets and variables ← Actions ← New repository secret. این چهار مورد را بساز:
   - `KEYSTORE_BASE64` ← محتوای فایل `voicex.jks.b64`
   - `KEYSTORE_PASSWORD` ← رمز keystore
   - `KEY_ALIAS` ← `voicex`
   - `KEY_PASSWORD` ← رمز کلید
3. از این به بعد workflow علاوه بر debug، فایل `VoiceX-release.apk` هم می‌سازد.

## ۴) انتشار روی صفحه‌ی Releases
```
git tag v1.0.0 && git push origin v1.0.0
```
APKها خودکار به Release همان نسخه اضافه می‌شوند.
