3D Solid Weapon 2 standalone на J2ME Loader 1.8.2-open
=======================================================

Что делает этот пакет:
- сохраняет штатный J2ME Loader 1.8.2-open и его M3G-библиотеки;
- встраивает исходный 3D Solid Weapon 2 JAR как asset;
- при первом запуске автоматически прогоняет JAR через штатный AppInstaller;
- при следующих запусках сразу запускает уже установленный MIDlet;
- отдельный package id: com.qmods.solidweapon2.

Основа должна быть commit/tag J2ME Loader 1.8.2: 7e978c5.

Применение:
1. Получить исходники J2ME-Loader 1.8.2 (commit 7e978c5).
2. Запустить:
   python apply_overlay.py /путь/к/J2ME-Loader
3. Собрать debug APK:
   ./gradlew assembleOpenDebug
4. APK будет в app/build/outputs/apk/open/debug/

Для release-сборки потребуется keystore.properties, как в оригинальном проекте.
Debug-сборка подписывается стандартным debug-ключом Android Gradle Plugin
(CI-сборке дополнительно нужна заглушка keystore.properties, чтобы прошла
стадия конфигурации Gradle — см. workflow).

ВАЖНО:
Это сделано именно на полном runtime 1.8.2-open, потому что игра использует
JSR-184/M3G. Нативные libjavam3g.so/libmicro3d.so не заменяются и не патчатся.
