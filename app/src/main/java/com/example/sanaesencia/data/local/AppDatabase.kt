package com.example.sanaesencia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sanaesencia.data.local.dao.CentroDao
import com.example.sanaesencia.data.local.entity.*
import com.example.sanaesencia.data.local.dao.CitaDao
import com.example.sanaesencia.data.local.dao.EspecialistaDao
import com.example.sanaesencia.data.local.dao.ServicioDao
import com.example.sanaesencia.data.local.entity.CitaEntity
import com.example.sanaesencia.data.local.entity.EspecialistaEntity
import com.example.sanaesencia.data.local.entity.ServicioEntity

// @Database le indica a Room qué entidades (tablas) forman el esquema y su versión.
// Si más adelante cambia alguna entidad, hay que subir "version" y definir una migración.
@Database(
    entities = [ServicioEntity::class, EspecialistaEntity::class, CitaEntity::class, UsuarioEntity::class,
        BoxEntity::class, MensajeEntity::class, EspecialistaServicioEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Room genera la implementación de los DAO durante la compilación mediante KSP.
    abstract fun centroDao(): CentroDao
    abstract fun servicioDao(): ServicioDao
    abstract fun especialistaDao(): EspecialistaDao
    abstract fun citaDao(): CitaDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE especialistas ADD COLUMN permitePresencial INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE especialistas ADD COLUMN permiteOnline INTEGER NOT NULL DEFAULT 1")
            }
        }
        val INTEGRIDAD = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) { crearSoporte(db); crearProtecciones(db) }
            override fun onOpen(db: SupportSQLiteDatabase) { crearProtecciones(db) }
        }

        // Conserva las citas canceladas, pero permite volver a reservar el horario liberado.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_citas_especialistaId_fechaHoraInicio")
                db.execSQL("CREATE INDEX index_citas_especialistaId_fechaHoraInicio ON citas(especialistaId, fechaHoraInicio)")
            }
        }

        /** Amplía el esquema sin eliminar cuentas antiguas ni citas. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE citas ADD COLUMN boxId INTEGER DEFAULT NULL")
                db.execSQL("UPDATE citas SET emailPaciente = lower(trim(emailPaciente))")
                db.execSQL("CREATE INDEX index_citas_boxId ON citas(boxId)")
                crearSoporte(db)
                // Asignar boxes libres a las citas existentes; las históricas conservan sus datos.
                db.query("SELECT id, fechaHoraInicio, duracionMin FROM citas WHERE modalidad = 'PRESENCIAL' AND estado != 'CANCELADA' ORDER BY fechaHoraInicio, id").use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0); val inicio = c.getLong(1); val fin = inicio + c.getInt(2) * 60000L
                        db.query("SELECT id FROM boxes WHERE activo = 1 AND NOT EXISTS (SELECT 1 FROM citas WHERE boxId = boxes.id AND estado != 'CANCELADA' AND fechaHoraInicio < ? AND fechaHoraInicio + duracionMin * 60000 > ?) ORDER BY id LIMIT 1", arrayOf(fin, inicio)).use { box ->
                            if (box.moveToFirst()) db.execSQL("UPDATE citas SET boxId = ? WHERE id = ?", arrayOf(box.getLong(0), id))
                        }
                    }
                }
                crearProtecciones(db)
            }
        }

        private fun crearSoporte(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS usuarios (email TEXT NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, telefono TEXT NOT NULL, hash TEXT NOT NULL, sal TEXT NOT NULL, rol TEXT NOT NULL, especialistaId INTEGER, fotoUri TEXT, activo INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS boxes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, activo INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS mensajes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, pacienteEmail TEXT NOT NULL, remitenteEmail TEXT NOT NULL, remitenteNombre TEXT NOT NULL, texto TEXT NOT NULL, creadoEn INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_mensajes_pacienteEmail ON mensajes(pacienteEmail)")
            db.execSQL("CREATE TABLE IF NOT EXISTS especialista_servicio (especialistaId INTEGER NOT NULL, servicioId INTEGER NOT NULL, PRIMARY KEY(especialistaId, servicioId))")
            db.execSQL("INSERT OR IGNORE INTO boxes(id,nombre,activo) VALUES (1,'Box 1',1),(2,'Box 2',1),(3,'Box 3',1)")
        }

        /** Segunda barrera de integridad, incluso si una escritura no pasa por el repositorio. */
        private fun crearProtecciones(db: SupportSQLiteDatabase) {
            for (evento in listOf("INSERT", "UPDATE")) {
                db.execSQL("""CREATE TRIGGER IF NOT EXISTS citas_sin_traslapes_${evento.lowercase()}
                    BEFORE $evento ON citas WHEN NEW.estado != 'CANCELADA'
                    BEGIN SELECT CASE WHEN EXISTS (
                        SELECT 1 FROM citas c WHERE c.id != NEW.id AND c.estado != 'CANCELADA'
                        AND c.fechaHoraInicio < NEW.fechaHoraInicio + NEW.duracionMin * 60000
                        AND c.fechaHoraInicio + c.duracionMin * 60000 > NEW.fechaHoraInicio
                        AND (c.especialistaId = NEW.especialistaId OR c.emailPaciente = NEW.emailPaciente
                            OR (NEW.boxId IS NOT NULL AND c.boxId = NEW.boxId))
                    ) THEN RAISE(ABORT, 'CONFLICTO_HORARIO') END; END""")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(INTEGRIDAD)
                    .build()
                    .also { INSTANCE = it } // se guarda para que las próximas llamadas la reutilicen
            }
        }
    }
}
