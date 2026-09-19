# MAX Web View

Простое Android-приложение (WebView), открывающее https://web.max.ru/.

- `minSdk`: 21 (Android 5.0+)
- `targetSdk`: 34
- Язык: Kotlin

## Возможности

- Полноэкранный WebView с поддержкой JavaScript, DOM storage и cookies.
- Pull-to-refresh и индикатор загрузки страницы.
- Навигация назад по истории WebView по системной кнопке «Назад».
- Ссылки на сторонние домены открываются во внешнем браузере, домен `max.ru` остаётся внутри приложения.
- Запрос разрешений камеры/микрофона для звонков и загрузка файлов из веб-интерфейса.
- Трафик разрешён только по HTTPS (`network_security_config.xml`).

## Сборка

```bash
cd webview-app
./gradlew assembleDebug
```

APK появится в `app/build/outputs/apk/debug/app-debug.apk`.

## CI

Сборка debug-APK выполняется автоматически в GitHub Actions
(`.github/workflows/webview-app-android.yml`) при пуше изменений в директорию
`webview-app/`. Готовый APK публикуется как артефакт workflow-запуска.
