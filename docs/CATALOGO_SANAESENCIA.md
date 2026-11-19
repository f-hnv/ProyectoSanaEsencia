# Catálogo del centro

Fuente de nombres, especialidades y estado de agenda: https://centrosanaesencia.cl/nuestro-profesionales/, consultada el 19/11/2026.

Se cargan 30 perfiles profesionales vinculados a 12 servicios. Siete perfiles aparecen con agenda pausada en la web: permanecen en administración, pero no se ofrecen para reservas nuevas. Los dos perfiles anunciados solo online no pueden recibir reservas presenciales.

Los $30.000 y 60 minutos por servicio son valores de demostración definidos por la autora del proyecto; no son tarifas verificadas del centro. Para las áreas cuya modalidad online no se anuncia, se ofrece atención presencial; terapia floral también permite online como configuración del proyecto.

Colores de referencia de la página: turquesa `#5EC7D6`, turquesa oscuro `#2CA0B0`, blanco y texto `#1D1D1F`. El tema los aplica a botones, bordes, selección y fondos suaves; los textos de botones usan tinta oscura para conservar contraste.

## Vincular una cuenta

1. Crear la cuenta con el correo y contraseña que definas, desde «Crear cuenta» de la aplicación.
2. Cerrar sesión y entrar con administración.
3. Buscar ese correo en «Roles de las cuentas».
4. Elegir «Especialista» y seleccionar su perfil existente por nombre. No crear un profesional nuevo si ya está en esta lista.
5. Guardar el rol. Al entrar con ese correo, la persona verá la agenda mensual del perfil seleccionado.

Los perfiles se cargan sin correos ni contraseñas. Cada perfil se vincula a una única cuenta. También se puede vincular una cuenta a un perfil con agenda pausada para consultar su historial.

La carga se ejecuta en cada arranque sin duplicar nombres ni relaciones, y conserva las cuentas, citas e identificadores anteriores. Los profesionales y servicios ficticios originales se retiran de la selección; sus reservas anteriores permanecen. La base Room pasa de versión 3 a 4 mediante migración, sin borrar datos.

Archivo de datos: `app/src/main/java/com/example/sanaesencia/data/local/seed/CatalogoCentro.kt`. La aplicación usa una copia local de la información; no consulta ni sincroniza agendas con la web.

Validación: 33 pruebas en el emulador y 10 unitarias aprobadas. Incluyen la importación repetida sin duplicados, conservación de reservas, migración de versiones 1 y 3, filtro por servicio y restricción de los perfiles solo online.
