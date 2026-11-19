# Roles y reservas en Android

Estos cambios funcionan con la base Room del dispositivo. No agregan backend ni conexión AWS.

## Paciente

- Las cuentas creadas desde «Crear cuenta» son pacientes.
- Conservan el inicio, catálogo, reserva, perfil y mensajes existentes.
- En la reserva pueden pulsar «Usar mi ubicación». Si ya existe permiso, se consulta una lectura actual al entrar.
- A más de 20 km (valor inicial del umbral de administración), se sugiere online y aparece «Elegir sesión online» cuando el servicio y el profesional admiten esa modalidad. Los perfiles anunciados solo online usan esa modalidad desde su selección.
- Un permiso denegado o un GPS sin lectura no impiden reservar. No se limita el país del paciente.
- Al confirmar presencial se asigna un box libre y se muestra en la confirmación y en Inicio. Online no utiliza box.
- Al reprogramar se puede cambiar también la modalidad si el servicio lo permite. Pasar a online libera el box; volver a presencial requiere un box libre.

## Especialista / terapeuta

- Inicia sesión con su propia cuenta: entra directamente en «Mi agenda mensual».
- Solo se consultan las citas del perfil profesional vinculado a esa cuenta, para el mes seleccionado.
- «Mes anterior» y «Mes siguiente» permiten cambiar de mes, también entre diciembre y enero.
- Cada cita muestra paciente, servicio, fecha y hora de Chile, duración, estado y box o modalidad online.
- No aparece el acceso a reservar citas como paciente.

## Administrador

Cuenta de demostración local existente: `admin.demo@sanaesencia.local` / `DemoCentro2026!`.

1. Entrar con esa cuenta. El inicio es ahora el panel de administración.
2. «Crear profesional y cuenta» conserva la creación de terapeutas con sus servicios.
3. En «Roles de las cuentas», buscar por nombre o correo.
4. Seleccionar «Paciente» o «Especialista». Para especialista, elegir el perfil profesional cuya agenda debe ver.
5. Pulsar «Guardar rol». La cuenta verá la pantalla correspondiente al iniciar sesión.

Una cuenta administradora conserva su rol. Dos cuentas no pueden vincularse al mismo perfil profesional. Cambiar un rol no borra reservas ni historiales.

## Archivos para entender el cambio

- `ui/agenda/UbicacionReserva.kt`: consulta y estados del GPS dentro de la reserva.
- `domain/model/VistasCuenta.kt`: acceso por rol y regla de sugerencia por distancia.
- `domain/model/PeriodoAgenda.kt`: límites del mes en la zona de Chile.
- `viewmodel/TerapeutaViewModel.kt` y `ui/terapeuta/TerapeutaScreen.kt`: consulta y presentación mensual.
- `data/repository/CuentaRepository.kt`: validación del cambio de rol por administrador.
- `data/repository/CitaRepository.kt`: asignación de box y cambio de modalidad.

Las cuentas y reservas siguen siendo locales: otro dispositivo no las recibe automáticamente.

## Comprobación manual

1. Registrar un paciente e iniciar sesión. Entrar a Servicios y elegir un servicio que admita online.
2. En la reserva, pulsar «Usar mi ubicación» y conceder el permiso. Con una ubicación a más de 20 km del centro, comprobar la sugerencia y pulsar «Elegir sesión online».
3. Para una cita presencial, seleccionar profesional, día y hora disponibles. Confirmar y revisar el número de box en Inicio.
4. Entrar como administrador y crear un profesional con sus servicios, correo y contraseña. Para cambiar una cuenta existente, usar «Roles de las cuentas» y vincular un perfil libre.
5. Reservar con ese profesional desde el paciente. Cerrar sesión y entrar con la cuenta del especialista: comprobar la cita en el mes correspondiente y su box o modalidad online.
6. Desde el paciente, reprogramar una cita con suficiente anticipación y cambiar su modalidad a online. Comprobar que muestre «Online · sin box».

Pruebas automatizadas: `PeriodoYModalidadTest`, `ReservaDistanciaTest`, `AgendaMensualRolesTest` y `VistasPorRolTest`, además de las pruebas existentes del proyecto. Cubren el límite de 20 km, permisos, cambio de año, filtros por profesional y mes, asignación de roles y liberación de boxes.

Validación del 19/11/2026: compilación debug correcta, 10 pruebas unitarias y 33 pruebas en Pixel 9 emulado sin fallos. Incluye lectura GPS con coordenadas de prueba, importación del catálogo público y el recorrido de asignar un rol desde administración y entrar con esa cuenta. Lint termina sin errores; mantiene advertencias del proyecto.
