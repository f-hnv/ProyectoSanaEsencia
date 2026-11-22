import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Configuración local excluida de Git. No registrar el valor de la clave.
val configuracionLocal = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val claveTrueWay = providers.environmentVariable("TRUEWAY_API_KEY")
    .getOrElse(configuracionLocal.getProperty("TRUEWAY_API_KEY", ""))
fun literalJava(valor: String) = "\"" + valor.replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

android {
    namespace = "com.example.sanaesencia"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.sanaesencia"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TRUEWAY_API_KEY", literalJava(claveTrueWay))
    }

    buildTypes {
        debug {
            buildConfigField("String", "RESERVAS_API_URL", "\"${providers.gradleProperty("RESERVAS_API_URL").getOrElse("http://127.0.0.1:8082/")}\"")
            buildConfigField("String", "USUARIOS_API_URL", "\"${providers.gradleProperty("USUARIOS_API_URL").getOrElse("http://127.0.0.1:8080/")}\"")
            buildConfigField("String", "CATALOGO_API_URL", "\"${providers.gradleProperty("CATALOGO_API_URL").getOrElse("http://127.0.0.1:8081/")}\"")
        }
        release {
            buildConfigField("String", "RESERVAS_API_URL", "\"${providers.gradleProperty("RESERVAS_API_URL").getOrElse("https://reservas.sanaesencia.invalid/")}\"")
            buildConfigField("String", "USUARIOS_API_URL", "\"${providers.gradleProperty("USUARIOS_API_URL").getOrElse("https://api.sanaesencia.invalid/")}\"")
            buildConfigField("String", "CATALOGO_API_URL", "\"${providers.gradleProperty("CATALOGO_API_URL").getOrElse("https://catalogo.sanaesencia.invalid/")}\"")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    ksp(libs.androidx.room.compiler)         // genera el código de los DAOs

}
