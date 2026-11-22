# Conexión de Android con los tres microservicios y TrueWay

**Actualización AWS:** las compilaciones ahora utilizan de forma predeterminada `https://54.204.182.160/usuarios/`, `/catalogo/` y `/reservas/`. Ver `CONEXION_AWS.md` para el despliegue y las pruebas actuales. Las instrucciones de localhost de este documento describen el entorno local anterior; para usarlo ahora se deben sobrescribir las tres URL mediante propiedades Gradle.

Registro, login, edición de nombre y apellido y asignación de roles usan `ms-usuarios` mediante Retrofit. El registro pide nombre, apellido, RUN válido sin puntos y con guion obligatorio, correo y contraseña de 8 a 64 caracteres (hasta 72 bytes UTF-8).

El servidor decide el ID, rol y perfil profesional. La copia de Room no autentica: ni las cuentas antiguas del celular ni la cuenta administradora local de demostración permiten entrar. Las credenciales Basic Auth quedan únicamente en memoria y se borran al salir o finalizar el proceso. Si Android cierra la app hay que iniciar sesión otra vez. No se agregó JWT ni Swagger.

## En esta PC

MySQL Server debe estar activo. Workbench puede cerrarse. Los servicios se ejecutan en 8080 (usuarios), 8081 (catálogo) y 8082 (reservas).

La compilación debug usa `http://127.0.0.1:8080/` para usuarios y `http://127.0.0.1:8081/` para catálogo y perfiles profesionales. Antes de ejecutar la app, se redirigen los puertos del dispositivo hacia la PC con ADB. Funciona tanto con emulador como con un celular por USB y no necesita exponer los servidores a la red Wi-Fi.

Desde la raíz del proyecto, con un único dispositivo/emulador conectado:

```powershell
.\preparar-conexion-local.ps1
.\gradlew.bat :app:installDebug
```

Después de desconectar/reiniciar el dispositivo hay que repetir las redirecciones. No guardar claves de MySQL en Android: la app solo habla HTTP con los servicios.

Android 17 y target SDK 37 restringen las conexiones LAN por defecto; el proyecto utiliza el túnel de desarrollo en localhost. No se desactivan protecciones del dispositivo ni se solicitan permisos de red local para producción. Referencia: https://developer.android.com/privacy-and-security/local-network-permission

## Roles y administración

El paciente ve sus reservas mensuales del servidor y el especialista las de su perfil vinculado. Administración busca una cuenta por su ID (visible en Mi perfil), consulta sus datos y cambia paciente/especialista en el servidor. El número del perfil profesional debe existir y estar activo en el catálogo; se valida antes de asignarlo. Los cambios aparecen al volver a iniciar sesión. La API de usuarios actual no ofrece listado/búsqueda por correo; por eso el panel busca por número de cuenta.

El panel administra cuentas, boxes, agendas y citas mediante sus APIs. La creación y edición de profesionales/servicios desde Android sigue pendiente. La foto y el teléfono siguen locales. El cambio de contraseña no se ofrece porque usuarios todavía no tiene ese endpoint.

## Catálogo conectado

La pantalla del paciente obtiene servicios y especialistas desde `ms-catalogo`. Precio, duración, descripción, modalidades y asignación profesional/servicio vienen del servidor. No se vuelven a importar los valores fijos del prototipo al iniciar la aplicación.

Cada servicio permite ver sus especialistas y elegir uno para pasar al formulario de agenda con ambos datos preseleccionados. Los especialistas sin asignación explícita no se ofrecen para ese servicio. La pantalla incluye carga, catálogo vacío, actualización y reintento. Si falla la conexión se puede leer la copia anterior, pero se requiere una actualización correcta antes de elegir. Antes de confirmar una cita se consultan nuevamente servicio y perfil; si cambió el precio, duración, modalidad o asignación se pide volver a elegir.

Room pasa de versión 4 a 5 conservando datos. Los IDs del servidor se vinculan a IDs locales propios: no sobrescriben por coincidencia servicios, especialistas o citas del prototipo anterior. Las actualizaciones usan UPDATE y no borran filas referenciadas por citas. Las bajas se conservan como inactivas y los precios decimales se guardan sin truncarlos. La caché se separa por URL de origen; al pasar a otro servidor no se reutilizan sus IDs. La selección inicial traduce los IDs locales a los IDs del servidor.

## Reservas conectadas

`ReservasApi`, `ReservasRepository` y los ViewModel de reserva/listado consumen `ms-reservas` en el puerto 8082. La disponibilidad, asignación de box, precio efectivo, creación, reprogramación, cancelación y agenda mensual provienen del servidor. Las escrituras no tienen respaldo local cuando falla HTTP. Las citas antiguas de Room se muestran por separado como historial anterior y no se migran automáticamente ni se asocian por similitud de nombre.

El paciente elige un cupo devuelto por la API; al confirmar se revalida el catálogo. La reprogramación conserva el ID y puede cambiar la modalidad si el profesional y servicio lo permiten. La respuesta muestra el box realmente asignado. La cancelación exige al menos 12 horas de anticipación tanto en la pantalla como en el servidor. El servidor impide superposiciones de paciente, profesional y box mediante una transacción con bloqueo en MySQL.

Se copiaron 120 cupos con fecha para 15 profesionales reales desde la agenda pública consultada el 22/11/2026. El archivo `ms-reservas/database/horarios-publicados-2026-11-22.json`, en el checkout de reservas, conserva la fuente y alcance. Los otros ocho perfiles no recibieron disponibilidad inventada. Se aceptan todas las horas publicadas observadas, incluidos sábados y horarios posteriores a las 18:00, según la decisión de la usuaria. Se mantienen duración de 60 minutos y precio del catálogo del proyecto. Un cupo publicado no genera horarios intermedios ni se repite automáticamente la semana siguiente.

Esto es una copia fechada de disponibilidad pública, no una sincronización con Medilink ni una reserva real en el centro. Los horarios deben actualizarse cuando cambien o venzan. La app conserva las citas únicamente en su propio servidor.

Desde la versión 1.3 el selector ofrece solo fechas con cupos disponibles dentro de los próximos 61 días. Primero consulta las fechas de la agenda del profesional y después verifica disponibilidad por servicio, modalidad, paciente y boxes, con hasta cuatro consultas simultáneas. Al cambiar profesional o modalidad se vuelve a consultar. Un fallo de red muestra reintento; no se interpreta como una agenda vacía. Al elegir un día se comprueban otra vez sus horas; la confirmación final sigue protegida por el servidor frente a reservas simultáneas.

## Pendiente de las etapas siguientes

- Creación y edición de profesionales/servicios desde administración usando las APIs de catálogo.
- Mensajes: aún locales.
- AWS y URL HTTPS de producción. HTTP se permite solo en debug. Release tiene direcciones `.invalid` hasta configurar servidores HTTPS reales con las propiedades Gradle anteriores. Esta etapa no produce un APK listo para publicar.

## Verificación

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Requiere emulador/dispositivo y, para AccesoBackendRealTest, usuarios y MySQL:
.\preparar-conexion-local.ps1
.\gradlew.bat :app:connectedDebugAndroidTest
```

Las pruebas de HTTP simulado validan los roles sin depender del servidor. Las de agenda/boxes locales se conservan. Las antiguas pruebas que creaban administradores y cambiaban contraseñas solo en Room se sustituyeron por pruebas de autenticación HTTP, edición del perfil, rechazo de contraseña con caché existente y navegación según el rol del servidor.

`AccesoBackendRealTest` recorre registro, login, catálogo real, selección de servicio/profesional, edición del perfil y logout contra MySQL. Crea una cuenta ficticia `android.e2e.*@example.com` y la desactiva al finalizar; no modifica cuentas personales ni confirma reservas. Las pruebas adicionales verifican relaciones, IDs coincidentes, actualización de precios, protección de citas históricas, migraciones, caída de conexión, caché, modalidad y estado vacío.

Verificación del 22/11/2026: compilación debug, lint y 51 pruebas unitarias correctas. `ReservasBackendRealTest` pasó contra los tres servicios y MySQL: creación presencial con box, reprogramación a online con el mismo ID, consulta del especialista vinculado, cancelación y disponibilidad liberada. Utilizó Samantha Bastías y su servicio real del catálogo con dos cuentas temporales, sin crear profesionales nuevos. Las cuentas quedaron desactivadas y las citas de prueba CANCELADAS. La ejecución es opt-in (`reservasE2e=true`) y requiere preparar un fixture privado en el dispositivo; no guarda credenciales en el repositorio. Las tres pruebas de `VistasPorRolTest` también pasaron. No se ha medido el porcentaje de cobertura ni se ha completado AWS o la firma release.

## API externa: TrueWay Directions con clave protegida

La pantalla Cómo llegar consulta un recorrido en auto hasta Caupolicán 220, Los Vilos. Al pulsar el botón se envían las coordenadas por HTTPS al servidor de SanaEsencia, que consulta TrueWay mediante RapidAPI. La pantalla informa de ambos destinatarios. No se envían nombre, RUN ni citas al proveedor.

Android utiliza `RutasApi.kt` con POST `api/rutas/sede` relativo a RESERVAS_API_URL. Envía la ubicación en el cuerpo JSON y la autorización Basic de la cuenta únicamente a SanaEsencia. `RutasRepository` comprueba la sesión, valida la respuesta y descarta resultados que llegan después de cerrar/cambiar sesión. `RutasViewModel` conserva los estados de carga, éxito y error, y evita el doble envío.

La clave externa se conserva como TRUEWAY_API_KEY en el archivo privado de entorno de reservas en AWS. No se lee desde local.properties, no existe el campo BuildConfig.TRUEWAY_API_KEY y la aplicación no realiza una llamada directa a RapidAPI. El cliente del proveedor en Java no reenvía las credenciales Basic del paciente.

La API protege el consumo con autenticación, un destino fijo, validación de coordenadas y límites de cinco intentos por cuenta/minuto y 400 intentos globales/día UTC en memoria. No guarda coordenadas en MySQL. La ubicación se envía en el body y las respuestas llevan no-store. La cuota del proveedor sigue aplicando; el contador de la instancia se reinicia al reiniciar el proceso.

Se usan route.distance en metros y route.duration en segundos. El recorrido por calles no sustituye la distancia en línea recta para la recomendación online de más de 20 km. Si falla la red, la sesión o el proveedor, se muestra un mensaje y permanece disponible Ver sede en mapas. No se inventa una distancia.

La estructura sigue las ramas EA3-Retrofit y EA3-ConsumoAPI de la profesora: interfaz, DTO, repositorio, ViewModel con StateFlow y pantalla Compose.

- https://github.com/nancyB3a/DSY1105_2026/tree/EA3-Retrofit
- https://github.com/nancyB3a/DSY1105_2026/tree/EA3-ConsumoAPI
- https://rapidapi.com/trueway/api/trueway-directions2

La configuración de firma y la instalación del APK se documentan en APK_FIRMADO.md. Las llaves privadas y sus contraseñas se conservan fuera de Git.
