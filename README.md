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

## TP2 — Ciclo Build-Measure-Learn

MVP con login por Firebase Authentication (`LoginActivity`, email y contraseña) y medición de uso
con Firebase Analytics (`Medicion`). Informe en `docs/TP2-informe.md`, guion de pruebas con
usuarios en `docs/TP2-guion-pruebas.md` y diagrama en `docs/arquitectura-tp2.svg`.

## TP3 — Estrategias de escalabilidad

API de reservas sin estado en `backend/` (FastAPI + Redis). `docker compose up -d --build` levanta
3 réplicas detrás de nginx; `python backend/pruebas.py` corre las pruebas del informe de punta a
punta (`backend/prueba_carga.py` mide el reparto y la latencia). El
autoescalado en Cloud Run está en `backend/cloudrun/service.yaml` (sin desplegar). Informe en
`docs/TP3-informe.md`, resultados en `docs/TP3-pruebas.log`.

## TP4 — Monetización

Banner de AdMob al pie de la lista, con los IDs de **prueba** públicos de Google. Estrategia de
monetización y pasos de Play Console en `docs/TP4-informe.md`.

## TP6 — Retención de usuarios

Recordatorio local del turno y por inactividad (WorkManager) y push con Firebase Cloud Messaging,
segmentado por temas según el deporte (`Notificaciones`, `MensajesPush`, `CanchasApp`). Con
`-PrecordatorioPrueba=true` los recordatorios salen a los pocos segundos. Informe en
`docs/TP6-informe.md`.

## TP7 — Integración de APIs y SDKs

Tres integraciones nuevas, ninguna con API key: mapa embebido en el detalle con **osmdroid**
(OpenStreetMap), pronóstico del turno con la API REST de **Open-Meteo** (`Clima`) y lista
ordenada por cercanía con **Google Play Services Location**. Informe en `docs/TP7-informe.md` y
diagrama en `docs/arquitectura-tp7.svg`. PDF: `python docs/generar_pdf.py TP7`.

## Compilar

Abrir la carpeta en Android Studio, o desde la terminal:

```
./gradlew assembleDebug
```

Requiere Android SDK Platform 36 y el `google-services.json` del proyecto de Firebase en `app/`. `minSdk` 26 (Android 8.0).
