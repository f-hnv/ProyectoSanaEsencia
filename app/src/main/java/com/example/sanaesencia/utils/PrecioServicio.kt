package com.example.sanaesencia.utils

import com.example.sanaesencia.data.local.entity.ServicioEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

fun ServicioEntity.precioExacto(): BigDecimal = precioServidor?.toBigDecimalOrNull() ?: precioClp.toBigDecimal()
fun ServicioEntity.precioFormateado(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CL"))
    .apply {
        maximumFractionDigits=if(precioExacto().stripTrailingZeros().scale()>0) 2 else 0
        minimumFractionDigits=maximumFractionDigits
    }
    .format(precioExacto())
