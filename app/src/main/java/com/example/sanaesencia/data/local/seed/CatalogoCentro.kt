package com.example.sanaesencia.data.local.seed

import androidx.room.withTransaction
import com.example.sanaesencia.data.local.AppDatabase
import com.example.sanaesencia.data.local.entity.*

/** Datos públicos consultados el 19/11/2026 en centrosanaesencia.cl/nuestro-profesionales/.
 * Precio y duración son valores de demostración solicitados por la autora del proyecto.
 * No crea cuentas ni contraseñas. La importación conserva reservas y perfiles existentes.
 */
object CatalogoCentro {
    data class Area(val clave: String, val nombre: String, val online: Boolean = false)
    data class Profesional(val nombre: String, val especialidad: String, val areas: List<String>,
        val pausado: Boolean = false, val soloOnline: Boolean = false)

    val areas = listOf(
        Area("medicina", "Medicina general y salud mental", true),
        Area("psicologiaInfantil", "Psicología infantojuvenil", true),
        Area("psicologiaAdultos", "Psicología de adolescentes y adultos", true),
        Area("ocupacional", "Terapia ocupacional"),
        Area("fonoaudiologia", "Fonoaudiología"),
        Area("nutricion", "Nutrición integral", true),
        Area("floral", "Terapia floral", true),
        Area("aromaterapia", "Aromaterapia"),
        Area("integrativa", "Terapia integrativa"),
        Area("acupuntura", "Acupuntura clínica"),
        Area("quiropraxia", "Quiropraxia clínica"),
        Area("matroneria", "Matronería y salud femenina")
    )

    val profesionales = listOf(
        Profesional("Dra. Keilen Sánchez Ríos", "Medicina general y salud mental", listOf("medicina")),
        Profesional("Dra. Camila Tabilo", "Medicina general y salud mental", listOf("medicina"), soloOnline = true),
        Profesional("Samantha Bastías", "Psicología clínica y educativa", listOf("psicologiaInfantil")),
        Profesional("Sebastián Olivares", "Psicología clínica y educativa", listOf("psicologiaInfantil")),
        Profesional("Nataly Godoy", "Psicología y terapia breve infantil", listOf("psicologiaInfantil"), pausado = true),
        Profesional("Anabel Guerra", "Psicología cognitivo conductual", listOf("psicologiaInfantil"), pausado = true),
        Profesional("Kristell Pinilla", "Neuropsicología infantil", listOf("psicologiaInfantil"), pausado = true),
        Profesional("Yanira Ferreira", "Psicología y psicoanálisis relacional", listOf("psicologiaAdultos")),
        Profesional("María José Mendicoa", "Psicología y psicoanálisis", listOf("psicologiaAdultos")),
        Profesional("Camila Soto", "Psicología con enfoque inclusivo", listOf("psicologiaAdultos"), soloOnline = true),
        Profesional("Francisca Villarroel", "Psicología y journals terapéuticos", listOf("psicologiaAdultos")),
        Profesional("Janis Herrera", "Psicología integrativa", listOf("psicologiaAdultos")),
        Profesional("Francisca Tornel", "Psicología clínica integral", listOf("psicologiaAdultos"), pausado = true),
        Profesional("Paula Orellana", "Psicología cognitivo conductual", listOf("psicologiaAdultos"), pausado = true),
        Profesional("Jeaseth Alam", "Terapia ocupacional", listOf("ocupacional")),
        Profesional("Pamela Vergara", "Terapia ocupacional", listOf("ocupacional")),
        Profesional("Sebastián Acuña", "Terapia ocupacional", listOf("ocupacional")),
        Profesional("Yanari Ramírez", "Terapia ocupacional", listOf("ocupacional")),
        Profesional("Natalia Rojas", "Fonoaudiología infantojuvenil", listOf("fonoaudiologia")),
        Profesional("Macarena González", "Fonoaudiología infantil", listOf("fonoaudiologia")),
        Profesional("Natalia Ortiz", "Fonoaudiología", listOf("fonoaudiologia")),
        Profesional("Gabriela Muñoz", "Fonoaudiología infantojuvenil", listOf("fonoaudiologia"), pausado = true),
        Profesional("Camila Collao C.", "Fonoaudiología", listOf("fonoaudiologia"), pausado = true),
        Profesional("Catalina Cobs", "Nutrición de adultos", listOf("nutricion")),
        Profesional("Betzabé Codoceo", "Nutrición infantil y materna", listOf("nutricion")),
        Profesional("Carolina Vega B.", "Nutrición", listOf("nutricion")),
        Profesional("Maria Ignacia", "Terapia floral, aromaterapia e integrativa", listOf("floral", "aromaterapia", "integrativa")),
        Profesional("Byron Sandoval", "Acupuntura", listOf("acupuntura")),
        Profesional("Daniel Díaz", "Quiropraxia clínica", listOf("quiropraxia")),
        Profesional("Carmen Paz Astudillo", "Matronería", listOf("matroneria"))
    )

    suspend fun instalar(db: AppDatabase) = db.withTransaction {
        val servicios = areas.associate { area ->
            val existente = db.servicioDao().obtenerPorNombre(area.nombre)
            area.clave to (existente?.id ?: db.servicioDao().insertar(ServicioEntity(
                nombre = area.nombre, descripcion = "Atención de ${area.nombre.lowercase()}.",
                categoria = area.nombre, duracionMin = 60, precioClp = 30_000, permiteOnline = area.online)))
        }
        profesionales.forEach { perfil ->
            val existente = db.especialistaDao().obtenerPorNombre(perfil.nombre)
            val id = existente?.id ?: db.especialistaDao().insertar(EspecialistaEntity(
                    nombre = perfil.nombre, especialidad = perfil.especialidad,
                    descripcion = if (perfil.pausado) "Agenda pausada · próximo retorno" else "",
                    activo = !perfil.pausado, permitePresencial = !perfil.soloOnline,
                    permiteOnline = perfil.areas.any { clave -> areas.first { it.clave == clave }.online }))
            val vinculados = db.centroDao().serviciosDeEspecialista(id)
            val nuevos = perfil.areas.map { servicios.getValue(it) }.filter { it !in vinculados }
            if (nuevos.isNotEmpty()) db.centroDao().vincularServicios(nuevos.map { EspecialistaServicioEntity(id, it) })
        }
        // Retirar del catálogo solo los ejemplos conocidos, conservando sus citas e identificadores.
        SeedData.especialistas.forEach { demo ->
            db.especialistaDao().obtenerPorNombre(demo.nombre)?.let { db.especialistaDao().actualizar(it.copy(activo = false)) }
        }
        SeedData.servicios.forEach { demo ->
            db.servicioDao().obtenerPorNombre(demo.nombre)?.let { db.servicioDao().actualizar(it.copy(activo = false)) }
        }
    }
}
