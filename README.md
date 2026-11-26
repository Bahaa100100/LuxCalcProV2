# LuxCalc Pro — تطبيق أندرويد عربي لتصميم وحساب الإضاءة الهندسية

## طريقة التشغيل وبناء ملف APK
1. فك ضغط الملف `LuxCalcPro_Android_Project.zip`.
2. افتح المجلد `LuxCalcPro` في **Android Studio** (إصدار Ladybug / Meerkat أو أحدث مع JDK 17).
3. انتظر اكتمال **Gradle Sync** (يستخدم Kotlin 2.0.21 + Jetpack Compose Material 3 + Room + DataStore + KSP).
4. لتشغيل اختبارات الحسابات الهندسية:
   `./gradlew testDebugUnitTest`
5. لبناء ملف APK جاهز للتثبيت على الهاتف:
   - من Android Studio: `Build > Build Bundle(s) / APK(s) > Build APK(s)`
   - أو من الطرفية: `./gradlew assembleDebug`
   - ستجد ملف الـ APK في: `app/build/outputs/apk/debug/app-debug.apk`

## ملاحظة حول Gemini AI (اختياري)
التطبيق يعمل بالكامل بدون إنترنت (Offline-First). لتفعيل التحليل المتقدم للفراغات غير المألوفة بـ Gemini AI، أدخل مفتاح API الخاص بك من صفحة **الإعدادات** داخل التطبيق (لا توجد مفاتيح مدمجة في الكود).
