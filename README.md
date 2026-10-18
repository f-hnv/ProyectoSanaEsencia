# Sanaesencia

Prototipo académico Android para consultar servicios, reservar sesiones y orientar la llegada al Centro Integrativo Sanaesencia de Los Vilos.

## Equipo

Grupo 4:

- Felipe Andres Araya Henriquez
- Antonia Sofia Munoz Pizarro

## Funcionalidades

- Registro e inicio de sesión local de demostración.
- Bienvenida animada tras validar las credenciales y reservas vinculadas al correo de la sesión activa.
- Pantalla “Contáctanos” desde Inicio y Login, con el WhatsApp publicado por el centro y alternativa de apertura en el navegador.
- Catálogo de servicios y selección de profesionales activos con su especialidad.
- Reserva por selección de día y botones de horas disponibles, sin escribir fechas ni horas.
- Horario configurado para el prototipo: lunes a viernes, 09:00–18:00, almuerzo 13:00–14:00 e inicios cada hora, en la zona `America/Santiago`.
- Disponibilidad según especialista y duración del servicio; exclusión de traslapes y anticipación mínima de 12 horas.
- Modalidad presencial u online cuando el servicio lo admite, con resumen antes de confirmar.
- Reprogramación conservando el servicio, profesional y modalidad originales; cancelación con 12 horas de anticipación.
- Persistencia en Room y actualización automática de horas ocupadas o liberadas.
- Pantalla “Cómo llegar”: permiso de ubicación, distancia aproximada en línea recta, estados de error y apertura del destino en una aplicación de mapas.
- Pantalla de perfil con solicitud de permiso de cámara, manejo de permiso denegado y selector de fotografías. La captura y el GPS en un teléfono físico siguen pendientes de comprobación.
- Transiciones entre pantallas y aparición animada del resumen de la reserva.
- Botones compartidos con bordes en salvia y celeste, iconos y estados de selección diferenciados por borde, fondo e indicador.

## Arquitectura

Kotlin, Jetpack Compose, Material 3 y MVVM, con inyección manual en `AppContainer` y `ViewModelFactory`. La política horaria se concentra en `HorariosAgenda`, la disponibilidad y la confirmación atómica en `CitaRepository`, y el estado de la agenda en `AgendaViewModel`.

La guía `docs/ARQUITECTURA.md` explica las carpetas, la función de las clases principales y los flujos de reserva y ubicación para la presentación del proyecto.

Room usa la versión 2 del esquema. La migración 1→2 conserva los registros y cambia el índice de horarios para permitir reutilizar una hora cancelada. Los traslapes se siguen verificando dentro de transacciones.

## Ejecutar

1. Abrir la carpeta del proyecto en Android Studio o en un IDE con soporte Android.
2. Sincronizar Gradle y disponer del SDK de Android API 37. La configuración del daemon del repositorio solicita JDK 25.
3. Conectar un dispositivo o iniciar un emulador compatible con Android API 24 o superior.
4. Ejecutar el módulo `app`. Para probar el flujo desde Inicio, registrar una cuenta ficticia e iniciar sesión.
5. Elegir **Agenda → servicio → especialista → modalidad → día → hora**. Revisar el resumen y confirmar.
6. Abrir **Cómo llegar** desde Inicio o desde el resumen de una reserva presencial para probar la ubicación.

Compilación y pruebas locales:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Pruebas de agenda en un dispositivo o emulador conectado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.sanaesencia.AgendaEspecialistasTest
```

La clase `GpsLecturaTest` comprueba el proveedor nativo y necesita una señal reciente. En el emulador se debe simular una ubicación durante la prueba; por ejemplo, desde sus controles extendidos de ubicación. Las pruebas de estados GPS usan un proveedor controlado para comprobar permisos denegados, ubicación desactivada, ausencia de señal y cálculo de distancia.

## Alcance y entrega

Los datos de servicios, profesionales y citas son ficticios. Los horarios y precios del prototipo no representan disponibilidad ni tarifas reales del centro. Las reservas se guardan localmente: no se envían al centro, a MEDIlink ni a un servidor. El inicio de sesión es una demostración local, no una autenticación de producción.

El sitio del centro se utilizó como referencia de contexto: https://centrosanaesencia.cl/. La implementación conserva las reglas acordadas para el proyecto académico.

## Documentación de Semana 9

La carpeta `docs/semana9` contiene acta de reunión, mockup, backlog priorizado y diseño de interfaces en PDF, sus versiones Markdown editables y capturas del prototipo. El acta registra la reunión del 24/09/2026 a las 20:15; el diseño continúa pendiente de respuesta y validación del cliente.

Las capturas se pueden regenerar de forma opcional en el emulador con datos ficticios:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.sanaesencia.CapturasDisenoTest -Pandroid.testInstrumentationRunnerArguments.capturas=true
```

Se guardan dentro del emulador en `Download/sanaesencia-*.png`. Esta tarea captura pantallas sin confirmar reservas ni enviar mensajes.

Antes de entregar se debe completar el equipo, verificar cámara y GPS en el dispositivo de defensa y revisar las evidencias de trabajo de ambos integrantes en GitHub y Trello. La integración REST, las notificaciones y la planificación de boxes descritas en el caso general no se implementan en esta mejora de agenda.
