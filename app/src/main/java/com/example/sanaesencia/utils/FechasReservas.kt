package com.example.sanaesencia.utils

import com.example.sanaesencia.domain.model.PeriodoAgenda
import java.text.SimpleDateFormat
import java.util.*

object FechasReservas {
    private val zona:TimeZone get()=TimeZone.getTimeZone("America/Santiago")
    private fun formato(patron:String)=SimpleDateFormat(patron,Locale.forLanguageTag("es-CL")).apply { timeZone=zona; isLenient=false }
    fun instante(iso:String):Long {
        require(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d{1,9})?)?").matches(iso)) { "Fecha de reserva no válida" }
        val normal=if(iso.length==16) "$iso:00" else iso.take(19)
        return requireNotNull(formato("yyyy-MM-dd'T'HH:mm:ss").parse(normal)).time
    }
    fun mostrar(iso:String,patron:String="dd/MM/yyyy HH:mm")=formato(patron).format(Date(instante(iso)))
    fun mes(periodo:PeriodoAgenda)=String.format(Locale.ROOT,"%04d-%02d",periodo.anio,periodo.mes)
    fun dias(ahora:Long=System.currentTimeMillis(),cantidad:Int=61):List<String> {
        val cal=Calendar.getInstance(zona).apply { timeInMillis=ahora; set(Calendar.HOUR_OF_DAY,12); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        return (0 until cantidad).map { formato("yyyy-MM-dd").format(cal.time).also { cal.add(Calendar.DAY_OF_MONTH,1) } }
    }
    fun diaVisible(dia:String)=mostrar("${dia}T12:00:00","EEE dd/MM")
    fun futura(reserva:com.example.sanaesencia.data.remote.ReservaRemota,ahora:Long=System.currentTimeMillis())=
        reserva.estado=="CONFIRMADA" && instante(reserva.inicio)>ahora
    fun cancelable(reserva:com.example.sanaesencia.data.remote.ReservaRemota,ahora:Long=System.currentTimeMillis())=
        reserva.estado=="CONFIRMADA" && instante(reserva.inicio)-ahora>=12*60*60*1000L
}
