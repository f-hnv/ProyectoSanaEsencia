package com.example.sanaesencia.domain.model

// Resultado tipado: el ViewModel decide qué mostrar sin depender de excepciones.
sealed class Resultado<out T> {
    data class Exito<T>(val datos: T) : Resultado<T>()
    data class Error(val error: ErrorAgenda) : Resultado<Nothing>()
}

// Cada error trae el mensaje en español, listo para mostrar en la UI.
sealed class ErrorAgenda(val mensaje: String) {
    data object HorarioFueraAtencion : ErrorAgenda("Selecciona uno de los horarios disponibles del centro")
    data object AnticipacionInsuficiente : ErrorAgenda("La reserva requiere al menos 12 horas de anticipación")
    data object CambioFueraPlazo : ErrorAgenda("Solo puedes cancelar o reprogramar hasta 12 horas antes de tu cita")
    data object FechaPasada : ErrorAgenda("La fecha y hora deben ser futuras")
    data object ServicioNoDisponible : ErrorAgenda("El servicio seleccionado no está disponible")
    data object EspecialistaNoDisponible : ErrorAgenda("El especialista seleccionado no está disponible")
    data object ModalidadNoDisponible : ErrorAgenda("Este servicio no se ofrece en la modalidad elegida")
    data object HorarioOcupado : ErrorAgenda("El especialista ya tiene una cita en ese horario")
    data object CitaNoEncontrada : ErrorAgenda("La cita no existe")
    data object EstadoInvalido : ErrorAgenda("Solo se pueden cancelar citas programadas")
}
