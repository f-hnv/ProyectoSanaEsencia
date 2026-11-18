package com.example.sanaesencia.data.local.dao
import androidx.room.*
import com.example.sanaesencia.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CentroDao {
    @Query("SELECT * FROM usuarios WHERE email = :email") suspend fun usuario(email: String): UsuarioEntity?
    @Query("SELECT * FROM usuarios WHERE email = :email") fun observarUsuario(email: String): Flow<UsuarioEntity?>
    @Query("SELECT * FROM usuarios ORDER BY nombre") fun usuarios(): Flow<List<UsuarioEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertarUsuario(usuario: UsuarioEntity)
    @Update suspend fun actualizarUsuario(usuario: UsuarioEntity)
    @Query("SELECT * FROM boxes WHERE activo = 1 ORDER BY id") suspend fun boxesActivos(): List<BoxEntity>
    @Query("SELECT * FROM boxes ORDER BY id") fun boxes(): Flow<List<BoxEntity>>
    @Query("SELECT * FROM boxes WHERE activo = 1 ORDER BY id") fun observarBoxesActivos(): Flow<List<BoxEntity>>
    @Update suspend fun actualizarBox(box: BoxEntity)
    @Query("SELECT * FROM mensajes WHERE pacienteEmail = :paciente ORDER BY creadoEn, id")
    fun mensajes(paciente: String): Flow<List<MensajeEntity>>
    @Query("SELECT DISTINCT pacienteEmail FROM mensajes ORDER BY pacienteEmail") fun conversaciones(): Flow<List<String>>
    @Insert suspend fun insertarMensaje(mensaje: MensajeEntity): Long
    @Insert suspend fun vincularServicios(vinculos: List<EspecialistaServicioEntity>)
    @Query("SELECT servicioId FROM especialista_servicio WHERE especialistaId = :id")
    suspend fun serviciosDeEspecialista(id: Long): List<Long>
}
