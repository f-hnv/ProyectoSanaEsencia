package com.example.sanaesencia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sanaesencia.data.local.dao.CitaDao
import com.example.sanaesencia.data.local.dao.EspecialistaDao
import com.example.sanaesencia.data.local.dao.ServicioDao
import com.example.sanaesencia.data.local.entity.CitaEntity
import com.example.sanaesencia.data.local.entity.EspecialistaEntity
import com.example.sanaesencia.data.local.entity.ServicioEntity

// @Database le indica a Room qué entidades (tablas) forman el esquema y su versión.
// Si más adelante cambia alguna entidad, hay que subir "version" y definir una migración.
@Database(
    entities = [ServicioEntity::class, EspecialistaEntity::class, CitaEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Room generacion y  la implementación de cada DAO en tiempo de compilación (gracias a KSP).
    abstract fun servicioDao(): ServicioDao
    abstract fun especialistaDao(): EspecialistaDao
    abstract fun citaDao(): CitaDao

    companion object {
        // Conserva las citas canceladas, pero permite volver a reservar el horario liberado.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_citas_especialistaId_fechaHoraInicio")
                db.execSQL("CREATE INDEX index_citas_especialistaId_fechaHoraInicio ON citas(especialistaId, fechaHoraInicio)")
            }
        }

        // PATRÓN SINGLETON: garantizamos que exista UNA sola instancia de la base de datos
        // en toda la app. Crear una instancia de Room es costoso (abre el archivo SQLite,
        // verifica el esquema) y tener varias conexiones abiertas al mismo archivo puede
        // producir inconsistencias y fugas de memoria.
        //
        // @Volatile: obliga a que cada hilo lea el valor real de INSTANCE desde la memoria
        // principal y no desde una caché propia del hilo. Así, cuando un hilo crea la base
        // de datos, los demás hilos ven inmediatamente que ya existe.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // 1er chequeo (sin bloqueo): si la instancia ya existe, se devuelve directo.
            // Es el caso más común y evita pagar el costo de sincronizar en cada llamada.
            return INSTANCE ?: synchronized(this) {
                // synchronized: solo UN hilo a la vez puede entrar a este bloque.
                // 2do chequeo (con bloqueo): si dos hilos pasaron el primer chequeo al mismo
                // tiempo, el segundo que entre encontrará la instancia ya creada por el
                // primero y no creará una duplicada. Esto se llama "double-checked locking".
                INSTANCE ?: Room.databaseBuilder(
                    // applicationContext y no el Activity: el Singleton vive tanto como la app,
                    // y retener un Activity provocaría una fuga de memoria (memory leak).
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sanaesencia_db" // nombre del archivo .db en el dispositivo
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it } // se guarda para que las próximas llamadas la reutilicen
            }
        }
    }
}
