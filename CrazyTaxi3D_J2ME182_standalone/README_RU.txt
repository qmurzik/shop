Crazy Taxi 3D standalone на J2ME Loader 1.8.2-open
==================================================

Что делает этот пакет:
- сохраняет штатный J2ME Loader 1.8.2-open и его M3G-библиотеки;
- встраивает исходный Crazy Taxi 3D JAR как asset;
- при первом запуске автоматически прогоняет JAR через штатный AppInstaller;
- при следующих запусках сразу запускает уже установленный MIDlet;
- отдельный package id: com.qmods.crazytaxi3d.

Основа должна быть commit/tag J2ME Loader 1.8.2: 7e978c5.

Применение:
1. Получить исходники J2ME-Loader 1.8.2 (commit 7e978c5).
2. Запустить:
   python apply_overlay.py /путь/к/J2ME-Loader
3. Собрать debug APK:
   ./gradlew assembleOpenDebug
4. APK будет в app/build/outputs/apk/open/debug/

Для release-сборки потребуется keystore.properties, как в оригинальном проекте.
Debug-сборка подписывается стандартным debug-ключом Android Gradle Plugin.

ВАЖНО:
Это сделано именно на полном runtime 1.8.2-open, потому что Crazy Taxi использует JSR-184/M3G.
Нативные libjavam3g.so/libmicro3d.so не заменяются и не патчатся.
