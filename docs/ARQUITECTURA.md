# Estructura de Sanaesencia

El código principal está en `app/src/main/java/com/example/sanaesencia`. Las carpetas coinciden con los paquetes declarados en los archivos. La organización separa presentación, estado, reglas de negocio, persistencia y recursos del dispositivo.

| Carpeta | Responsabilidad | Ejemplos |
| --- | --- | --- |
| `ui` | Pantallas de Jetpack Compose, agrupadas por funcionalidad | `agenda/AgendaScreen.kt`, `login/LoginScreen.kt`, `contacto/ContactoScreen.kt` |
| `ui/components` | Controles visuales compartidos | `SanaButtons.kt` |
| `ui/theme` | Colores, tipografía y configuración de Material 3 | `Color.kt`, `Type.kt`, `Theme.kt` |
| `viewmodel` | Estado de pantalla y coordinación de las acciones del usuario | `AgendaViewModel.kt`, `LoginViewModel.kt`, `RegistroViewModel.kt` |
| `navigation` | Rutas, argumentos y transiciones entre pantallas | `Navigation.kt` |
| `data/repository` | Acceso a datos y coordinación de operaciones persistentes | `CitaRepository.kt`, `ServicioRepository.kt` |
| `data/local` | Base de datos Room y su migración | `AppDatabase.kt` |
| `data/local/entity` | Tablas persistidas por Room | `ServicioEntity.kt`, `EspecialistaEntity.kt`, `CitaEntity.kt` |
| `data/local/dao` | Consultas e instrucciones de acceso a las tablas | `CitaDao.kt`, `EspecialistaDao.kt` |
| `data/local/relation` | Resultados de consultas con relaciones o agregaciones | `CitaConDetalle.kt`, `ResumenPaciente.kt` |
| `data/local/seed` | Datos ficticios de inicialización para el prototipo | `SeedData.kt` |
| `domain/model` | Modelos, resultados y política horaria del negocio | `Resultado.kt`, `HorariosAgenda.kt` |
| `domain/analytics` | Cálculo de indicadores de continuidad del tratamiento | `CalculadorRiesgo.kt` |
| `nativo` | Acceso al proveedor de ubicación de Android | `LocationHelper.kt` |
| `di` | Construcción y reutilización de dependencias | `AppContainer.kt` |
| `utils` | Funciones auxiliares de validación y distancia | `ValidacionesAgenda.kt`, `Geo.kt` |

## Cómo explicar MVVM en la presentación

La pantalla muestra datos y envía eventos. El ViewModel conserva el estado y decide qué operación debe ejecutarse. El repositorio consulta los DAO o realiza transacciones. Room transforma las entidades y consultas en acceso a SQLite. Las actualizaciones regresan al estado del ViewModel y Compose recompone la vista.

## Ejemplo con una reserva

1. `AgendaScreen` muestra el servicio y permite elegir especialista, modalidad, día y hora.
2. `AgendaViewModel` mantiene la selección y observa los horarios del profesional.
3. `HorariosAgenda` define los bloques de atención, el almuerzo, la zona horaria y la anticipación requerida.
4. `CitaRepository` valida la reserva y comprueba los traslapes dentro de una transacción.
5. `CitaDao` guarda la cita y Room notifica las actualizaciones.
6. El ViewModel publica éxito o error y la pantalla muestra la respuesta correspondiente.

## Ejemplo con la ubicación

`SedeScreen` solicita el permiso al usuario. `SedeViewModel` pide una lectura al proveedor `LocationHelper` y convierte el resultado en un estado de pantalla. `Geo` calcula y formatea la distancia a la sede. La interfaz diferencia permiso denegado, GPS desactivado, lectura no disponible y ubicación obtenida.

## Dos clases que conectan las capas

- `SanaesenciaApp` inicia el contenedor y carga los datos de prueba en segundo plano.
- `AppContainer` comparte Room, repositorios y proveedor de ubicación. `ViewModelFactory` entrega esas dependencias a los ViewModels que tienen parámetros.

## Límites que conviene explicar correctamente

- Las citas se guardan localmente con Room; esta versión no las sincroniza con el centro real.
- El registro y el acceso son una demostración local con SharedPreferences.
- La foto de perfil se conserva durante la vida del ViewModel; no tiene persistencia permanente implementada.
- La lógica de alertas existe, pero no hay una pantalla de administración conectada a la navegación.
- Los nombres de archivos no siempre son nombres de clases: `SanaButtons.kt` agrupa controles Compose, y `Enums.kt` agrupa enumeraciones relacionadas.

## Pruebas

`app/src/test` contiene pruebas unitarias. `app/src/androidTest` contiene las pruebas que requieren Android, Room o la interfaz. Estas carpetas pertenecen a conjuntos de código distintos y no deben mezclarse con `src/main`.
