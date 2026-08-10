# Panda — Asistente de voz local para Android

Esqueleto modular, **100% on-device**, sin ninguna API de IA en la nube.
Todo el procesamiento de voz (wake word, reconocimiento y síntesis) ocurre
en el propio teléfono.

## Pipeline implementado

```
"Oye Panda" (wake word, Vosk)
      │
      ▼
Reconocimiento de voz del comando (Vosk STT)
      │
      ▼
ActionRegistry resuelve qué acción ejecutar
      │
      ▼
Respuesta hablada (Android TextToSpeech)
```

Acción incluida de ejemplo: **"¿qué hora es?"** → Panda responde la hora actual.

## Estructura del proyecto

```
app/src/main/java/com/panda/
├── core/          Interfaces (contratos) — el sistema depende de esto, no de Vosk/Android directamente
├── wakeword/       Implementación de detección de "Oye Panda" (Vosk)
├── stt/            Implementación de reconocimiento de comandos (Vosk)
├── tts/            Implementación de voz hablada (Android TextToSpeech)
├── actions/        Acciones ejecutables + registro central
├── service/        Foreground service que mantiene todo corriendo
└── ui/             MainActivity (pide permisos, arranca el servicio)
```

Cada módulo (wakeword, stt, tts, actions) implementa una interfaz de `core/`.
Para cambiar de motor de voz, o para agregar visión, juegos o automatización
más adelante, se agregan módulos nuevos sin tocar los existentes.

## Paso obligatorio antes de compilar: modelo de voz

Este esqueleto usa **Vosk** para reconocimiento offline. Necesitas descargar
un modelo (no se incluye aquí porque pesa decenas de MB) y colocarlo en:

```
app/src/main/assets/model-wakeword/
```

Modelo recomendado para español (pequeño, ~50MB):
`vosk-model-small-es-0.42` — descárgalo desde https://alphacephei.com/vosk/models
y descomprime su contenido directamente dentro de `model-wakeword/`
(debe quedar `model-wakeword/am/`, `model-wakeword/conf/`, etc.).

## Compilar

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/`.

## Cómo agregar una acción nueva

1. Crear una clase en `actions/` que implemente `PandaAction`.
2. Registrarla en la lista de `ActionRegistry`.
3. Listo — el orquestador no necesita cambios.

## Próximos pasos (fuera de este esqueleto, a propósito)

- Visión (cámara)
- Juegos / entretenimiento
- Automatización avanzada (controlar otras apps, rutinas, etc.)

Estos se agregarán como módulos nuevos bajo `com.panda.<módulo>/`
implementando sus propias interfaces en `core/`, siguiendo el mismo patrón.

