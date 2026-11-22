# Android conectado a AWS Academy

La compilación debug y release utiliza estas URL predeterminadas:

| Servicio | URL base |
|---|---|
| Usuarios | https://54.204.182.160/usuarios/ |
| Catálogo | https://54.204.182.160/catalogo/ |
| Reservas | https://54.204.182.160/reservas/ |

Se configuran en `app/build.gradle.kts` y se incorporan a BuildConfig. `ConexionBackend.kt` las entrega a Retrofit. Los endpoints son relativos (`api/...`), por lo que conservan el prefijo de cada servicio al pasar por Nginx. No se desactiva la validación de certificados TLS.

## Infraestructura configurada por la estudiante

- EC2 Ubuntu 24.04 en us-east-1, con IP elástica 54.204.182.160.
- MySQL local al servidor, con tres bases y tres usuarios técnicos independientes.
- Contraseñas generadas en el servidor y guardadas en `/etc/sanaesencia/*.env`, con permisos restringidos.
- Servicios systemd `sana-usuarios`, `sana-catalogo` y `sana-reservas`.
- Los JAR escuchan en 127.0.0.1:8080/8081/8082. Nginx publica solo HTTP/HTTPS; HTTP redirige a HTTPS salvo la validación del certificado.


## Datos y cuentas

Se creó una cuenta administradora y cargó el catálogo real con `/api/admin/catalogo/importar-centro`. Se adaptaron los IDs de la nube antes de cargar 120 cupos con fecha para 15 profesionales reales. No se inventaron horarios para los ocho perfiles sin disponibilidad pública consultable.

Los cupos proceden de una copia de la agenda pública el 22/11/2026; no se repiten automáticamente cada semana y deben actualizarse cuando venzan o cambien.

## Ejecutar Android

Con AWS activo y acceso a internet, se puede ejecutar Run desde Android Studio o instalar la compilación debug. No se requiere `preparar-conexion-local.ps1`, redirecciones ADB ni mantener encendidos los servicios de la PC para conectarse a AWS.

El laboratorio y la instancia deben estar activos para atender peticiones. La IP elástica mantiene la dirección, pero no enciende el servidor ni evita las restricciones del laboratorio. Si la instancia está apagada no puede atender peticiones ni renovar certificados. Antes de la defensa, comprobar el laboratorio, los servicios, el certificado y el flujo de reserva desde un celular.


## Comprobaciones

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.sanaesencia.ReservasAwsTest -Pandroid.testInstrumentationRunnerArguments.awsE2e=true
```

La prueba opt-in crea una cuenta temporal, usa un profesional real con cupos futuros, confirma una cita presencial con box, la reprograma a online conservando el ID, la cancela y comprueba la disponibilidad liberada. Al finalizar desactiva su propia cuenta. No utiliza la contraseña del administrador ni crea perfiles profesionales nuevos. Requiere que existan dos cupos futuros apropiados en los próximos siete días.

Resultado del 22/11/2026: pasaron la compilación debug, lint y 51 pruebas unitarias. `ReservasAwsTest` pasó en Pixel 9 API 37 contra los tres microservicios de AWS, sin redirecciones ADB a la PC. La reserva de demostración quedó CANCELADA y la cuenta temporal desactivada. `RutasAwsTest` comprobó también la consulta de TrueWay a través del servidor desde Android. La estudiante confirmó el éxito de `certbot renew --cert-name sanaesencia --dry-run`. La generación y verificación release se documenta en `APK_FIRMADO.md`; queda pendiente la prueba física en la tablet.

## Clave externa protegida y firma

La clave de RapidAPI se trasladó al servidor y se retiró de BuildConfig. Android usa `POST /api/rutas/sede` autenticado en reservas; el cliente dedicado del servidor llama a TrueWay con la clave de entorno. Ver `APK_FIRMADO.md` para generar el APK release con el JKS privado y realizar la prueba de instalación en la tablet. No publicar la llave privada ni sus contraseñas en un repositorio público.
