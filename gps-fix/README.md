# Анализ и исправление проблемы с GPS (com.instashopper)

## Что было сделано

На вход было передано 4 файла: `classes.dex`, `classes2.dex`, `classes3.dex`, `classes4.dex`
(мультидекс APK приложения `com.instashopper`, React Native / Kotlin, сборочный флейвор
`shopper_locator_universalRelease`). Файлы были дизассемблированы в smali (`baksmali`),
после чего был проанализирован модуль трекинга геолокации `com.instashopper.locator`
(классы пакетов `com.instashopper.locator.*`, `T7.*`, `U7.*`, `V7.*`, `R7.*` (Google-провайдер),
`S7.*` (Huawei-провайдер), `M7.*`).

## Причина плохой работы GPS

Фоновый трекинг местоположения реализован через `LocatorForegroundService`
(foreground service, вызывает `startForeground()`). Служба (пере)запускается в трёх местах:

- `RebootReceiver` — перезапуск трекинга после перезагрузки устройства;
- `CheckLocationStatusWorker` — периодический "супервизор" на WorkManager (каждые ~17 минут),
  который проверяет, что служба жива, и перезапускает её, если нет;
- `LocatorModule` — обычный запуск трекинга из JS по команде пользователя.

Все три места запускают службу через общий помощник `V7/r.smali` (`LV7/r;->b(Context, Intent)`).
В нём была следующая (ошибочная) логика:

```smali
.method private static final a(Landroid/content/Context;)Z
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I
    const/16 v1, 0x1d          ; 29 (Android 10)
    if-ge v0, v1, :cond_8
    const/4 p0, 0x1
    return p0                  ; SDK < 29 -> всегда "можно"
    :cond_8
    ; SDK >= 29 -> "можно" только если процесс приложения сейчас на переднем плане
    invoke-virtual {v0, p0}, LM7/d;->a(Landroid/content/Context;)Z
    move-result p0
    return p0
.end method

.method public static final b(Landroid/content/Context;Landroid/content/Intent;)V
    invoke-static {p0}, LV7/r;->a(Landroid/content/Context;)Z
    move-result v0
    if-eqz v0, :cond_13
    invoke-virtual {p0, p1}, Landroid/content/Context;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;
    :cond_13
    return-void
.end method
```

Проблемы:

1. **Служба запускается через `Context.startService()`, а не через `startForegroundService()`.**
   Начиная с Android 8.0 (API 26), вызов `startService()` из фонового контекста
   (`BroadcastReceiver`, `WorkManager.Worker` и т.п.) для сервиса, который затем
   вызывает `startForeground()`, приводит к `IllegalStateException:
   "Not allowed to start service Intent ...: app is in background"`. Такое
   исключение либо роняет вызывающий код, либо просто "проглатывается" —
   в любом случае служба не запускается.

2. **Проверка версии API стоит неверно (29 вместо 26).** Ограничение на
   `startService()` из фона действует с Android 8.0 (`API 26`), а не с
   Android 10 (`API 29`). На устройствах с Android 8/9 код считал, что
   "запускать можно всегда", хотя фактически уже действовало ограничение
   ОС — на части устройств это тоже приводило к падению перезапуска.

3. **На Android 10+ при "не на переднем плане" перезапуск просто тихо
   пропускается.** Вместо того чтобы использовать предназначенный именно
   для этого API — `startForegroundService()` (который как раз можно
   вызывать из фона, включая `BroadcastReceiver` после загрузки устройства
   и `WorkManager`), код проверял "виден ли процесс пользователю" и, если
   нет, просто ничего не делал.

### Как это проявляется у пользователей

Трекинг корректно стартует, когда пользователь сам открывает приложение и
включает его (в этот момент приложение на переднем плане — путь через
`LocatorModule` "случайно" работает). Но как только:

- телефон перезагружается (`RebootReceiver` срабатывает, когда приложение
  не запущено пользователем), или
- систему убивает фоновый сервис (Doze / оптимизация батареи / нехватка
  памяти), а супервизор (`CheckLocationStatusWorker`) пытается перезапустить
  его в фоне,

— перезапуск службы молча не происходит (или падает с исключением), и
фоновая передача геолокации перестаёт работать до тех пор, пока
пользователь вручную не откроет приложение снова. Это и есть основная
причина жалоб "GPS работает плохо" — трекинг действительно перестаёт
обновляться в фоне и сам не восстанавливается.

## Исправление

`Context.startForegroundService()` **специально предназначен** для запуска
foreground-сервисов из фонового контекста (ровно случаи `RebootReceiver` и
`WorkManager`), при условии, что сама служба вызывает `startForeground()` в
течение 5 секунд после запуска — что `LocatorForegroundService` и так делает.
Поэтому проверка "на переднем ли плане приложение" не нужна вообще: нужно
всегда использовать `startForegroundService()` начиная с API 26, а на более
старых версиях — обычный `startService()` (как и делает
`androidx.core.content.ContextCompat.startForegroundService()`).

Патч в `V7/r.smali` (см. `patch/V7/r.smali` — полный исправленный файл,
`patch/V7-r.smali.diff` — diff):

```smali
.method public static final b(Landroid/content/Context;Landroid/content/Intent;)V
    ...
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I
    const/16 v1, 0x1a                 ; 26 (Android 8.0, Build.VERSION_CODES.O)
    if-lt v0, v1, :cond_e
    invoke-virtual {p0, p1}, Landroid/content/Context;->startForegroundService(Landroid/content/Intent;)Landroid/content/ComponentName;
    goto :goto_11
    :cond_e
    invoke-virtual {p0, p1}, Landroid/content/Context;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;
    :goto_11
    return-void
.end method
```

Вспомогательный метод `a()` (проверка "приложение на переднем плане") и
зависимость от `LM7/d` в этом файле больше не нужны и удалены; сам класс
`M7/d` не удалялся (используется в другом месте — событие видимости для JS).

Если в проекте есть Kotlin-исходники (а не только скомпилированный APK),
эквивалентное исправление в исходном коде — заменить

```kotlin
context.startService(intent)
```

на

```kotlin
ContextCompat.startForegroundService(context, intent)
```

в функции, которая (пере)запускает `LocatorForegroundService` (используется
из `RebootReceiver`, `CheckLocationStatusWorker` и модуля `LocatorModule`).

## Пересборка

Файл `classes2.dex` в этой папке — пересобранный (через `smali`/`baksmali`
2.5.2) декс с применённым патчем; остальные классы не менялись. Его нужно
подставить вместо оригинального `classes2.dex` в APK и переподписать сборку
(файлы манифеста, ресурсов и keystore не передавались вместе с dex-файлами,
поэтому пересобрать и подписать сам APK в этой сессии невозможно — это
может сделать команда, у которой есть доступ к проекту/сборочному пайплайну).

## Что не является причиной (проверено и признано корректным)

В процессе анализа были также проверены и признаны рабочими:
- выбор провайдера геолокации Google/Huawei по коду `GoogleApiAvailability`
  (`T7/g.smali`);
- фильтрация "свежести" координат по времени (`T7/i.b`, `U7/c.b`);
- построение `LocationRequest` (приоритет/интервалы) для Google-провайдера
  (`R7/j.smali`);
- маршрутизация колбэков между разовым запросом местоположения и
  постоянным трекингом (`R7/j$a`/`R7/j$b`, поля `g`/`h`, `i`/`j`);
- хранение флага "трекинг должен быть запущен" в `SharedPreferences`
  (`T7/s.smali`);
- расписание WorkManager-супервизора (`T7/u.smali`).
