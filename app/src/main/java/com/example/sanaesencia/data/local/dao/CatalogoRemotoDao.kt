package com.example.sanaesencia.data.local.dao

import androidx.room.*
import com.example.sanaesencia.data.local.entity.*

@Dao
interface CatalogoRemotoDao {
    @Query("SELECT * FROM catalogo_vinculos WHERE origen=:origen")
    suspend fun vinculos(origen:String):List<CatalogoVinculoEntity>
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun vincular(vinculo:CatalogoVinculoEntity)
    @Query("SELECT * FROM catalogo_sincronizacion WHERE origen=:origen")
    suspend fun sincronizacion(origen:String):CatalogoSincronizacionEntity?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun sincronizar(meta:CatalogoSincronizacionEntity)
    @Query("DELETE FROM especialista_servicio WHERE especialistaId IN (:ids)")
    suspend fun limpiarServiciosDeEspecialistas(ids:List<Long>)
}
