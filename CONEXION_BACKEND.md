# Conexión de Android con los tres microservicios y TrueWay

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

## API externa: TrueWay Directions

La pantalla «Cómo llegar» permite consultar un recorrido en auto desde la ubicación actual hasta Caupolicán 220, Los Vilos. El botón explica que al consultar se envían las coordenadas a TrueWay mediante RapidAPI. No se envían nombres, RUN, correo, citas ni credenciales de los microservicios. La ubicación y el resultado no se guardan en Room.

El recorrido sigue el patrón de las ramas EA3-Retrofit y EA3-ConsumoAPI de la profesora: interfaz Retrofit, DTO, repositorio, ViewModel con StateFlow y pantalla Compose con carga, éxito y error/reintento.

- Proveedor: https://rapidapi.com/trueway/api/trueway-directions2
- GET `https://trueway-directions2.p.rapidapi.com/FindDrivingRoute`
- Query `stops`: `latitudOrigen,longitudOrigen;latitudSede,longitudSede`.
- Headers `X-RapidAPI-Host` y `X-RapidAPI-Key`.
- Se usan `route.distance` (metros) y `route.duration` (segundos). Se muestra tiempo estimado redondeado hacia arriba; no es navegación en vivo.
- El recorrido por calles no sustituye la distancia en línea recta para la recomendación de atención online a más de 20 km.
- Si falta clave, no hay ruta, falla la red o se agota la cuota, se muestra un mensaje y sigue disponible «Ver sede en mapas». No se inventa un resultado.

### Configuración de desarrollo

Agregar `TRUEWAY_API_KEY=valor_de_tu_clave` sin comillas en `local.properties` y volver a compilar/instalar. También se admite la variable de entorno `TRUEWAY_API_KEY`. No subir ese archivo ni claves al repositorio. Se utiliza un cliente HTTP independiente sin logging de claves ni autorización Basic de SanaEsencia.

La clave incluida en BuildConfig forma parte del APK; excluirla de Git no la vuelve secreta dentro de una aplicación distribuida. Antes de entregar públicamente el APK, revisar las restricciones y el mecanismo de protección de credenciales del proveedor. El despliegue público y la firma release siguen pendientes.

### Verificación realizada en RapidAPI

Se probó `stops=-31.900000,-71.500000;-31.909825,-71.512501` el 21 de noviembre de 2026. La respuesta fue HTTP 200, `route.distance=1825` y `route.duration=369`. Esto verifica ese recorrido de Los Vilos; no implica cobertura comprobada para todo Chile. La integración Android requiere configurar la clave y probarse también desde el dispositivo.

Ejemplos de referencia:
- https://github.com/nancyB3a/DSY1105_2026/tree/EA3-Retrofit
- https://github.com/nancyB3a/DSY1105_2026/tree/EA3-ConsumoAPI

Verificación de la integración Android: compilación debug, lint y 41 pruebas unitarias correctas (9 de TrueWay). En el emulador pasaron las 4 pruebas de RecorridoSedeTest, incluida la llamada real con la clave configurada, y las 2 pruebas existentes de SedeGpsTest. Se reinstaló la aplicación debug al terminar. No se generó un APK release firmado ni se realizaron commits en esta etapa.
