# Diseño de interfaces gráficas
Sanaesencia | Grupo 4

## Paleta
- Primario salvia: #45664E
- Secundario celeste: #426575
- Contenedor salvia: #D4E8D3
- Contenedor celeste: #D1EAF5
- Fondo general: #FDFBF7

## Componentes
- SanaPrimaryButton: acción principal con borde y esquinas de 16 dp.
- SanaOutlineButton: acción secundaria con borde celeste y fondo suave.
- SanaChoiceChip: borde de 1,5 dp; selección con borde de 2 dp y fondo salvia.
- MenuButton: tarjeta de acceso con icono circular y borde salvia-celeste.
- Tarjetas de especialista con radio y estado seleccionado.

## Flujo
Acceso > Inicio > Catálogo > Especialista > Modalidad > Día > Hora > Resumen > Confirmación.

## Estados
Sin selección, cargando, sin horas, selección completa, error y éxito. La confirmación se bloquea hasta completar los datos. Los mensajes e iconos acompañan el estado y la selección no depende solo del color.

## Verificaciones pendientes
Validación del cliente; captura real de cámara; GPS en teléfono físico; tipografía ampliada y pantalla del dispositivo de defensa.
