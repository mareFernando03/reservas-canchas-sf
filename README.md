# Reservas Canchas SF

App Android para reservar canchas (fútbol 5, fútbol 7, pádel) en San Francisco, Córdoba.
Trabajo práctico de **Arquitecturas Móviles** — UTN FRSF, 2026.

## TP1 — Desarrollo de aplicaciones móviles

Aplicación básica con `AndroidManifest`, tres Activities e Intents:

| Pantalla | Qué hace |
|---|---|
| `MainActivity` | Lista de canchas. Abre el detalle con un Intent explícito. |
| `DetalleCanchaActivity` | Llamar al club (`ACTION_DIAL`), ver en el mapa (`ACTION_VIEW` + `geo:`) y reservar (Intent con resultado). |
| `ReservaActivity` | Día, hora y duración. Devuelve el turno con `setResult` y lo comparte con `ACTION_SEND`. |

Todas heredan de `CicloDeVidaActivity`, que registra cada callback del ciclo de vida en Logcat
(filtro `CicloDeVida`). La duración del turno se guarda en `onSaveInstanceState` para que
sobreviva a la rotación. Los datos de canchas son de ejemplo y viven en memoria.

## Compilar

Abrir la carpeta en Android Studio, o desde la terminal:

```
./gradlew assembleDebug
```

Requiere Android SDK Platform 36. `minSdk` 26 (Android 8.0).
