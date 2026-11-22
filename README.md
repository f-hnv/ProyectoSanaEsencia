# SanaEsencia — Evaluación Parcial 4, DSY1105

Aplicación Android académica con Kotlin/Jetpack Compose, tres microservicios Spring Boot y MySQL en AWS. Consulta servicios del centro, gestiona reservas y muestra el recorrido a la sede. No es el sistema oficial de Sanaesencia ni realiza reservas en Medilink.

## Equipo

Grupo 4: **Felipe Andres Araya Henriquez** y **Antonia Sofia Munoz Pizarro**.

## APK y ejecución

- Versión **1.3**, código 4, Android mínimo **7.0 / API 24**.
- [Archivo APK firmado](entregables/SanaEsencia-1.3.apk).
- Descarga: https://54.204.182.160/descargas/SanaEsencia-1.3.apk
- SHA-256: `2f5094b11e9bad836021d4f4ce540a6273e9ca1b15bfd7c501c18c8b3e6759e5`.

Descargar y abrir el APK. Sobre la 1.2, aceptar Actualizar sin desinstalar. Se requiere internet y AWS activo; la PC de desarrollo no necesita estar encendida. Hacer commit no actualiza una APK instalada.

Para ejecutar el código: abrir la carpeta raíz en Android Studio, instalar SDK API 37 y sincronizar. El wrapper usa Gradle 9.6.0 y el daemon JDK 25 configurado en el proyecto. Ejecutar `app` en un dispositivo API 24+. Registrar una cuenta propia; no se publica una contraseña administradora.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## Funcionalidades actuales

| Área | Implementación |
| --- | --- |
| Usuarios | Registro y login en AWS; validación del RUN; BCrypt en el servidor; roles PACIENTE, ESPECIALISTA y ADMIN. |
| Catálogo | Servicios y profesionales reales de referencia desde ms-catalogo; copia de lectura en Room. |
| Agenda | Solo fechas con cupos disponibles para el profesional y modalidad en los próximos 61 días. |
| Reservas | Crear, listar, reprogramar conservando el ID y cancelar; persistencia central en MySQL. |
| Cruces y boxes | Se evitan cruces del paciente, profesional y box; el servidor asigna box para citas presenciales. Online no usa box. |
| Especialista | Agenda mensual con paciente, fecha, hora, modalidad, box y estado; configuración de disponibilidad. |
| Administración | Vincular cuentas a fichas profesionales, gestionar boxes, agendas y citas. |
| API externa | Recorrido TrueWay con distancia/duración. Retrofit llama a ms-reservas y el servidor protege la clave de RapidAPI. |
| Dispositivo | GPS, permisos, cámara/galería y apertura de mapas/WhatsApp. |

Cancelar requiere al menos **12 horas** de anticipación. Las fechas y horas publicadas se copiaron de la agenda web consultada el 22/11/2026: 120 cupos para 15 profesionales; no se inventaron cupos para los demás. Incluye los días y horas observados en esa consulta. Los precios/duraciones pertenecen al proyecto. **No existe sincronización con Medilink** y los cupos deben actualizarse cuando venzan.

El registro crea PACIENTES. Para vincular una profesional: el administrador busca su número de cuenta, comprueba el correo y asigna ESPECIALISTA con el ID de su ficha del catálogo. Ella cierra sesión y vuelve a entrar. Escribir el mismo nombre no otorga permisos.

## Conexiones y endpoints

```text
Android → ViewModel → Repository → Retrofit → HTTPS/Nginx en AWS
  /usuarios/ → ms-usuarios :8080 → sanaesencia_usuarios
  /catalogo/ → ms-catalogo :8081 → sanaesencia_catalogo
  /reservas/ → ms-reservas :8082 → sanaesencia_reservas
                                  └→ TrueWay / RapidAPI
```

Base pública: `https://54.204.182.160/`. Las URL están en `app/build.gradle.kts`. Basic Auth se transmite mediante HTTPS; las credenciales de sesión están en memoria. No se implementó JWT.

| Servicio | Endpoint relativo a su URL base | Uso |
| --- | --- | --- |
| Usuarios | `POST api/usuarios/registro`, `POST api/usuarios/login` | Registro y acceso. |
| Usuarios | `GET/PUT/DELETE api/usuarios/{id}` | Consultar, editar o desactivar cuenta autorizada. |
| Usuarios | `PATCH api/admin/usuarios/{id}/rol` | Vincular rol y especialistaId. |
| Catálogo | `GET api/servicios`, `GET api/especialistas` | Servicios y profesionales. |
| Reservas | `GET api/agenda/{id}`, `GET api/disponibilidad` | Agenda publicada y disponibilidad efectiva. |
| Reservas | `GET/POST api/reservas` | Citas del mes y creación. |
| Reservas | `PUT/DELETE api/reservas/{id}` | Reprogramar y cancelar. |
| Reservas | `PATCH api/reservas/{id}/estado` | Estado de atención según permisos. |
| Reservas | `POST api/rutas/sede` | Distancia y duración de TrueWay mediante el servidor. |

Proveedor externo: `GET https://trueway-directions2.p.rapidapi.com/FindDrivingRoute`, parámetro `stops` y encabezados X-RapidAPI-Host / X-RapidAPI-Key. La clave está en el entorno privado de AWS, **no en el APK ni en Git**. Android consume el proxy propio con Retrofit; Spring Boot consulta TrueWay. Este recorrido se documenta para contrastarlo con el requisito de API externa de la pauta.

Backend: [Sanaesencia-Microservicios](https://github.com/AMunozPSo/Sanaesencia-Microservicios), ramas `ms-usuarios`, `ms-catalogo` y `ms-reservas`.

Room versión 5 conserva recursos locales y una copia del catálogo con vínculos explícitos de IDs. Las reservas nuevas pertenecen al servidor; el historial local se muestra aparte. Los mensajes de esta versión permanecen en cada dispositivo y no constituyen un chat entre teléfonos.

## Firma y pruebas

La APK 1.3 está firmada con JKS, RSA 3072, alias `sanaesencia`. La configuración está en `app/build.gradle.kts`; `generar-apk-firmado.ps1` carga la llave privada y ejecuta assembleRelease y lintRelease. La llave y su contraseña se conservan fuera de Git, en `%LOCALAPPDATA%/SanaEsencia/firma-release`.

```powershell
.\generar-apk-firmado.ps1
.\gradlew.bat :app:createDebugUnitTestCoverageReport
```

El 22/11/2026 pasaron **92 pruebas unitarias**. También se verificaron el selector visual de fechas, el recorrido real TrueWay por AWS y la reserva/reprogramación/cancelación contra AWS. La APK pasó compilación release, lint, firma, ausencia de secretos y comparación SHA-256 de su descarga. Las cuentas temporales se desactivan y las citas de prueba quedan canceladas. La estudiante confirmó instalar la 1.2; queda confirmar el flujo de la 1.3 en un dispositivo físico.

**Cobertura JaCoCo de lógica:** 86,87% de líneas (1224/1409), 81,75% de instrucciones (15399/18837) y 61,08% de ramas (954/1562). Incluye todos los archivos de `viewmodel`, `data/repository`, `domain` y `utils`, incluidos sus tipos auxiliares. No es un porcentaje de toda la interfaz Compose ni afirma 80% de ramas. Se usa `includeNoLocationClasses` para contar también las clases cargadas por Robolectric. El informe se genera en `app/build/reports/jacoco/logica/index.html` y `app/build/reports/jacoco/logica.xml`.

```powershell
.\gradlew.bat :app:verificarCoberturaLogica
```

La verificación falla si las líneas o instrucciones de esos paquetes bajan del 80%. Las pruebas nuevas cubren validación de cuentas, permisos, perfiles, persistencia con Room, sincronización del catálogo, cruces, reprogramación, disponibilidad, estados de ViewModel, riesgo de abandono y ubicación.

Evidencias para revisar sin ejecutar Gradle: [reporte HTML de cobertura](docs/entrega/cobertura/index.html), [XML de cobertura](docs/entrega/cobertura-logica.xml), [cifras resumidas](docs/entrega/resultado-cobertura.json) y [certificado de firma verificado](docs/entrega/FIRMA_VERIFICADA.txt).

![Evidencia técnica del APK firmado y del almacén JKS](docs/entrega/evidencia-apk-firma.png)

El equipo informa avances compartidos en **Microsoft Teams**, sin tablero Trello previo. Debe conservar esa evidencia real y completar o aclarar con la docente el requisito explícito de Trello, sin atribuir fechas inexistentes.

Documentación actual: [guía PDF de estudio y defensa](docs/entrega/Guia_SanaEsencia.pdf), [conexión Android/backend](CONEXION_BACKEND.md), [AWS](CONEXION_AWS.md) y [APK/firma](APK_FIRMADO.md). Antes de defender: activar Learner Lab y EC2, comprobar HTTPS/servicios, mantener cupos vigentes y probar el APK entregado. La IP elástica no enciende el servidor.

<details>
<summary>Documentación histórica de la Evaluación Parcial 2 — no describe la versión actual</summary>

# Sanaesencia (prototipo anterior)

Aplicación Android de demostración para consultar servicios, reservar sesiones y orientar la llegada al Centro Integrativo Sanaesencia de Los Vilos. Desarrollada como proyecto académico de **DSY1105 — Desarrollo de Aplicaciones Móviles**, Evaluación Parcial 2.

## Integrantes

**Grupo 4**

- Felipe Andres Araya Henriquez
- Antonia Sofia Munoz Pizarro

## Objetivo

Facilitar la reserva de una sesión mediante un flujo guiado: elegir servicio, especialista, modalidad, día y una hora disponible. La aplicación incorpora validaciones, almacenamiento local y recursos del dispositivo para apoyar la experiencia del paciente.

## Funcionalidades implementadas

| Área | Funcionalidad |
| --- | --- |
| Registro y acceso | Formularios con mensajes por campo, confirmación de contraseña e inicio de sesión local. |
| Inicio | Saludo al paciente, próximas sesiones y accesos al catálogo, agenda, perfil, ubicación y contacto. |
| Catálogo | Consulta de servicios, descripción, duración, precio de demostración y acceso a la reserva. |
| Agenda | Selección de profesional con su especialidad, modalidad, día y botones de horas disponibles; resumen antes de confirmar. |
| Gestión de citas | Reserva, cancelación y reprogramación; disponibilidad que se actualiza al ocupar o liberar una hora. |
| Ubicación | Solicitud de permiso, lectura del GPS, distancia aproximada en línea recta y apertura de la sede en una aplicación de mapas. |
| Perfil | Captura de foto con cámara, selección desde galería y manejo de permiso denegado. |
| Contacto | Apertura de WhatsApp con un texto editable y alternativa de navegador; el usuario decide si envía el mensaje. |
| Diseño e interacción | Material 3, paleta salvia y celeste, botones compartidos con borde y estados seleccionados, bienvenida animada y transiciones. |

### Reglas de agenda

- Atención del prototipo: **lunes a viernes de 09:00 a 18:00**, con almuerzo de **13:00 a 14:00**, en la zona `America/Santiago`.
- Se eligen bloques con inicio cada hora; el paciente no escribe la fecha ni la hora manualmente.
- La disponibilidad considera el especialista, la duración del servicio, el cierre de atención, el almuerzo y las citas vigentes.
- Se requieren al menos **12 horas de anticipación** para reservar, cancelar o reprogramar.
- La confirmación comprueba los traslapes dentro de una transacción Room.
- Una hora cancelada puede volver a reservarse.
- La modalidad online está disponible cuando el servicio la admite.
- La reprogramación conserva el servicio, el profesional y la modalidad originales.

## Tecnologías y arquitectura

- **Kotlin**, **Jetpack Compose** y **Material Design 3**.
- **MVVM**, ViewModel, coroutines y Flow para el estado y la actualización de la interfaz.
- **Room / SQLite** para servicios, especialistas y citas.
- **SharedPreferences** para la cuenta y la sesión local de demostración.
- **Navigation Compose** para la navegación.
- Inyección manual de dependencias mediante `AppContainer` y `ViewModelFactory`.
- API de ubicación de Android, permisos en tiempo de ejecución, cámara con `FileProvider` y selector de fotografías.

Flujo principal de datos:

```text
Pantalla Compose → ViewModel → Repositorio → DAO → Room
                       ↑                       |
                       └──── actualizaciones ──┘
```

La política horaria se concentra en `HorariosAgenda`; la disponibilidad y las operaciones atómicas, en `CitaRepository`; y la selección del paciente, en `AgendaViewModel`.

Room utiliza el esquema **versión 2**. La migración 1 → 2 conserva los datos y modifica el índice de horarios para permitir nuevas reservas sobre citas canceladas, manteniendo la validación de traslapes en las transacciones.

### Organización del código

Dentro de `app/src/main/java/com/example/sanaesencia/`:

| Carpeta | Responsabilidad |
| --- | --- |
| `ui/` | Pantallas agrupadas por funcionalidad. |
| `ui/components/` y `ui/theme/` | Controles compartidos, colores, tipografía y tema. |
| `viewmodel/` | Estado, validaciones y acciones de las pantallas. |
| `navigation/` | Rutas y navegación. |
| `data/repository/` | Acceso y coordinación de operaciones sobre los datos. |
| `data/local/` | Base Room, entidades, DAO, relaciones y datos iniciales. |
| `domain/` | Reglas horarias, modelos e indicadores de continuidad. |
| `nativo/` | Proveedor de ubicación. |
| `di/` y `utils/` | Dependencias y funciones auxiliares. |

Consultar la [guía de arquitectura](docs/ARQUITECTURA.md) para revisar las clases principales y los flujos de reserva y ubicación.

## Requisitos para ejecutar

- Android Studio con soporte para la configuración Gradle incluida en este repositorio.
- **Android SDK API 37** instalado desde SDK Manager.
- **JDK 25** para el daemon de Gradle, según `gradle/gradle-daemon-jvm.properties`.
- Emulador o dispositivo con **Android API 24 o superior**.
- Conexión a internet para la primera sincronización y descarga de dependencias.

Se incluye **Gradle Wrapper 9.6.0**; no es necesario instalar Gradle por separado. La compatibilidad Java del código está configurada en Java 11, que es una configuración distinta del JDK utilizado por el daemon.

### Instalación y ejecución

1. Clonar este repositorio o descargar y descomprimir su código fuente.
2. En Android Studio, elegir **File → Open** y seleccionar la carpeta que contiene `settings.gradle.kts`, `gradlew.bat` y `app/`.
3. Revisar la ruta del SDK local y sincronizar Gradle. El archivo `local.properties` depende de cada computador y no se versiona.
4. Iniciar un emulador o conectar un dispositivo compatible.
5. Seleccionar el módulo **app** y pulsar **Run**.

Para compilar desde PowerShell, en la raíz del proyecto:

```powershell
.\gradlew.bat :app:assembleDebug
```

El APK de pruebas se genera en `app/build/outputs/apk/debug/app-debug.apk`. Es una compilación debug para demostración, no una publicación en una tienda.

## Recorrido de demostración

1. Abrir **Registro** y crear una cuenta ficticia. Se aceptan correos terminados en `@duoc.cl`, `@profesor.duoc.cl`, `@gmail.com` o `@hotmail.com`; la contraseña requiere al menos seis caracteres y confirmación coincidente.
2. Iniciar sesión con esa misma cuenta y observar la bienvenida animada. No existe una cuenta de acceso predefinida.
3. Desde **Agenda**, elegir servicio, especialista, modalidad, día y una hora disponible. Revisar el resumen y confirmar.
4. Volver a **Inicio** para consultar la cita y probar la cancelación o reprogramación respetando las 12 horas de anticipación.
5. Abrir **Cómo llegar**, conceder ubicación y comprobar la distancia. En un emulador debe simularse una ubicación; también se pueden revisar los estados de permiso denegado y GPS desactivado.
6. Abrir **Perfil** y probar cámara o galería. Si se deniega el permiso de cámara, se mantiene la alternativa de galería.
7. Abrir **Contáctanos** para comprobar el enlace externo. La aplicación no envía mensajes automáticamente.

Los datos de ejemplo se cargan cuando la tabla de servicios está vacía. Las citas iniciales pertenecen a pacientes ficticios y no equivalen a credenciales de acceso; cada nueva reserva se asocia al correo de la sesión activa.

## Pruebas y verificación

### Compilación, pruebas unitarias y análisis estático

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

### Pruebas funcionales en Android

Con un emulador o dispositivo conectado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.notClass=com.example.sanaesencia.CapturasDisenoTest'
```

Esta ejecución excluye las capturas opcionales. `GpsLecturaTest` necesita una lectura GPS reciente: en el emulador se debe proporcionar una ubicación desde sus controles extendidos durante la prueba. Las pruebas de estados utilizan además un proveedor controlado para comprobar errores y cálculo de distancia.

**Última verificación: 18/10/2026**

- Compilación debug y compilación de pruebas Android: correctas.
- **5 pruebas unitarias aprobadas** y **13 pruebas funcionales aprobadas en emulador**, sin fallos en esa ejecución.
- Cobertura funcional de pruebas: selección y persistencia del especialista, disponibilidad, reutilización de horas canceladas, migración Room, acceso, contacto, permiso de cámara denegado y ubicación.
- Análisis estático: **0 errores y 31 advertencias** de mantenimiento, recursos y dependencias.
- La captura con cámara y el GPS en un teléfono físico **siguen pendientes de comprobación**. Las pruebas de emulador no sustituyen esa revisión.

### Capturas opcionales

Para generar evidencia visual con datos ficticios:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.sanaesencia.CapturasDisenoTest' '-Pandroid.testInstrumentationRunnerArguments.capturas=true'
```

Las imágenes se guardan en el emulador, en `Download/sanaesencia-*.png`. Estas pruebas no confirman reservas ni envían mensajes. Deben ejecutarse por separado con el argumento `capturas=true`; sin él, el generador se omite por una condición de JUnit que el reporte utilizado puede mostrar como fallo.

## Evidencia visual

Capturas del prototipo de Semana 9:

| Acceso | Reserva |
| --- | --- |
| ![Pantalla de acceso](docs/semana9/login.png) | ![Selección de especialista y horario](docs/semana9/agenda.png) |

También están disponibles las capturas de [inicio](docs/semana9/inicio.png), [resumen de reserva](docs/semana9/resumen.png) y [contacto](docs/semana9/contacto.png).

## Documentación y planificación

| Documento | Contenido |
| --- | --- |
| [Acta de reunión](docs/semana9/01_Acta_reunion_cliente.pdf) | Reunión del 24/09/2026 a las 20:15 y asuntos pendientes de validación. |
| [Mockup](docs/semana9/02_Mockup_Sanaesencia.pdf) | Propuesta de pantallas y recorrido del paciente. |
| [Backlog priorizado](docs/semana9/03_Backlog_priorizado.pdf) | Funcionalidades y prioridades del proyecto. |
| [Diseño de interfaces](docs/semana9/04_Diseno_interfaces.pdf) | Criterios visuales, controles y evidencia de pantallas. |
| [Guía del tablero](docs/semana9/LEEME_PARA_EL_TABLERO.md) | Orientación para organizar la evidencia de Semana 9. |

La carpeta [docs/semana9](docs/semana9) incluye también versiones Markdown editables. El diseño fue enviado y continúa pendiente de respuesta y aprobación del cliente. El backlog documentado no sustituye la evidencia de tareas compartidas en Trello o una herramienta equivalente.

## Alcance y limitaciones

- Los servicios, profesionales, pacientes y citas iniciales son ficticios. Los precios y horarios no representan tarifas ni disponibilidad real del centro.
- Las reservas se guardan en el dispositivo con Room. No existe sincronización entre dispositivos ni envío a un servidor, al centro o a MEDIlink.
- El registro conserva una sola cuenta local de demostración en SharedPreferences; registrar otra reemplaza esa cuenta. Las credenciales se almacenan sin un mecanismo de autenticación de producción. Utilizar exclusivamente datos y contraseñas ficticios.
- La foto de perfil se mantiene en el estado del ViewModel; no tiene persistencia permanente implementada.
- La distancia mostrada es aproximada en línea recta, no una estimación de ruta o tiempo de viaje.
- La integración REST, las notificaciones y la planificación de boxes del caso general no están implementadas. Existe lógica de indicadores de continuidad, pero no una pantalla de administración conectada a la navegación.
- Abrir WhatsApp, el navegador o mapas depende de las aplicaciones disponibles y de la conectividad del dispositivo.

El [sitio del centro](https://centrosanaesencia.cl/) se utilizó como referencia de contexto. Este repositorio corresponde a una solución académica y no a un sistema oficial de reservas.

</details>

