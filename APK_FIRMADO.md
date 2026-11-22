# APK release firmado de SanaEsencia

Versión actual: 1.3, versionCode 4. Paquete: com.example.sanaesencia. Android mínimo: 7.0 (API 24). La estudiante instaló la versión 1.2 en una Lenovo Tab P11 con Android 14; la 1.3 utiliza la misma firma y puede instalarse como actualización.

## Firma y respaldo privado

La llave se conserva fuera del repositorio:

`%LOCALAPPDATA%\SanaEsencia\firma-release\sanaesencia-release.jks`

Alias: `sanaesencia`. Formato JKS, RSA 3072. `password.clixml`, en esa carpeta, contiene la contraseña cifrada para el usuario actual de Windows. `LEEME-PRIVADO.txt` explica cómo verla de forma privada y respaldar la firma. Un archivo clixml de este tipo no se descifra automáticamente en otra PC; conservar también la contraseña de forma segura para recuperar la firma fuera de esta instalación de Windows.

No publicar el JKS, su contraseña ni los archivos privados en GitHub o en el servidor de descargas. La llave se debe conservar para firmar las próximas actualizaciones de la misma app.

## Volver a generar el APK

Desde PowerShell en la raíz del proyecto:

```powershell
.\generar-apk-firmado.ps1
```

El script lee la contraseña cifrada y pasa las variables `SANA_RELEASE_STORE_FILE`, `SANA_RELEASE_STORE_PASSWORD` y `SANA_RELEASE_KEY_PASSWORD` al proceso de Gradle. No escribe contraseñas en el código. Ejecuta assembleRelease y lintRelease. Sin esa configuración privada, una compilación manual release puede quedar sin firmar.

Archivo generado: `app/build/outputs/apk/release/app-release.apk`. No confundir con la compilación debug ni con un archivo que diga unsigned.

La optimización parcial experimental está desactivada: la primera prueba release detectó un `IllegalAccessError` al iniciar OkHttp por una clase de Kotlin. Se mantienen reglas de ProGuard para conservar los contratos JSON si se habilita una optimización compatible en el futuro. La firma release y mantener la clave en AWS no dependen de la ofuscación.

## Protección de RapidAPI

El APK no contiene la clave de RapidAPI. `RutasApi` llama al endpoint autenticado de `ms-reservas`; el servidor es quien usa TrueWay. La app informa que al consultar comparte las coordenadas con SanaEsencia y con el proveedor. Los límites de consultas y la clave privada se configuran en el servidor.

## Instalar en el dispositivo

Descarga verificada: https://54.204.182.160/descargas/SanaEsencia-1.3.apk

Versión 1.3 (código 4), 14.1 MB. SHA-256: `2f5094b11e9bad836021d4f4ce540a6273e9ca1b15bfd7c501c18c8b3e6759e5`.

Esta actualización muestra solo fechas con cupos disponibles para profesional y modalidad en los próximos 61 días. Pasaron 54 pruebas unitarias, la prueba visual del selector y la reserva/reprogramación/cancelación real contra AWS. Conserva la misma firma de la versión 1.2 y se instala encima sin desinstalar.

El 22/11/2026 pasaron assembleRelease, lintRelease, verificación de firma, inspección de ausencia de secretos y arranque del APK release en el emulador. La descarga HTTPS devolvió el mismo SHA-256. Queda pendiente la prueba física en la Lenovo Tab P11 con Android 14.

1. Mantener el Learner Lab, EC2 y sus servicios activos.
2. Descargar el APK desde el enlace HTTPS de SanaEsencia o copiarlo por USB a Descargas del dispositivo
3. Abrir el archivo. Si Android solicita permiso, autorizar a ese navegador/gestor de archivos para instalar esta aplicación.
4. Instalar y abrir SanaEsencia. No requiere conectar el dispositivo a Android Studio ni usar ADB.
5. Crear una cuenta de paciente en AWS o iniciar sesión con una cuenta existente allí. La cuenta ADMIN muestra administración; para reservar usar PACIENTE.
6. Probar catálogo, cupos, una reserva con box, modificación, cancelación con 12 horas y Cómo llegar.

La APK queda instalada aunque AWS esté apagado, pero las funciones del servidor requieren internet y la instancia activa. Los horarios copiados tienen fechas concretas y deben mantenerse actualizados.
